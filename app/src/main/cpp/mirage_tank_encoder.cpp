#include <jni.h>
#include <android/bitmap.h>
#include <algorithm>
#include <arm_neon.h>
#include <omp.h>
#include <vector>
#include <cmath>

inline uint8x8_t neon_to_gray(uint8x8_t r, uint8x8_t g, uint8x8_t b) {
    const uint16_t W_R = 19595;
    const uint16_t W_G = 38469;
    const uint16_t W_B = 7472;

    uint16x8_t r16 = vmovl_u8(r);
    uint16x8_t g16 = vmovl_u8(g);
    uint16x8_t b16 = vmovl_u8(b);

    uint32x4_t sum_lo = vmull_n_u16(vget_low_u16(r16), W_R);
    sum_lo = vmlal_n_u16(sum_lo, vget_low_u16(g16), W_G);
    sum_lo = vmlal_n_u16(sum_lo, vget_low_u16(b16), W_B);

    uint32x4_t sum_hi = vmull_n_u16(vget_high_u16(r16), W_R);
    sum_hi = vmlal_n_u16(sum_hi, vget_high_u16(g16), W_G);
    sum_hi = vmlal_n_u16(sum_hi, vget_high_u16(b16), W_B);

    uint16x4_t res_lo = vmovn_u32(vshrq_n_u32(sum_lo, 16));
    uint16x4_t res_hi = vmovn_u32(vshrq_n_u32(sum_hi, 16));

    return vqmovn_u16(vcombine_u16(res_lo, res_hi));
}
int toGrayScalar(int r, int g, int b) {
    int gray = (r * 19595 + g * 38469 + b * 7472) >> 16;
    return std::min(255, gray);
}
struct ScaleXTable {
    std::vector<int> src_indices;
    std::vector<int> weights;
};
ScaleXTable precompute_x_table(int src_w, int dst_w) {
    ScaleXTable table;
    table.src_indices.resize(dst_w);
    table.weights.resize(dst_w);

    float x_ratio = dst_w > 1 ? (float)(src_w - 1) / (dst_w - 1) : 0;

    for (int x = 0; x < dst_w; ++x) {
        float src_xf = x * x_ratio;
        int src_x = (int)src_xf;
        float diff = src_xf - src_x;

        table.src_indices[x] = src_x;
        // 定点化：0.0~1.0 -> 0~256
        table.weights[x] = (int)(diff * 256.0f);
    }
    return table;
}
inline uint8_t fast_lerp(int a, int b, int w_256) {
    return (uint8_t)(a + (( (b - a) * w_256 ) >> 8));
}
void get_scaled_row(const uint8_t* srcPixels, int src_stride, int src_w, int src_h,
                    int dst_y, int dst_w, float y_ratio,
                    const ScaleXTable& x_table, uint32_t* out_row_buffer) {
    float src_yf = dst_y * y_ratio;
    int src_y = (int)src_yf;
    int y_weight = (int)((src_yf - src_y) * 256.0f);
    int y0 = src_y;
    int y1 = std::min(src_y + 1, src_h - 1);
    const uint32_t* row0 = (const uint32_t*)(srcPixels + y0 * src_stride);
    const uint32_t* row1 = (const uint32_t*)(srcPixels + y1 * src_stride);
    const int* p_idx = x_table.src_indices.data();
    const int* p_w = x_table.weights.data();
    for (int x = 0; x < dst_w; ++x) {
        int sx = p_idx[x];
        int xw = p_w[x];

        int sx_next = (sx + 1 < src_w) ? sx + 1 : sx;

        uint32_t c00 = row0[sx];
        uint32_t c01 = row0[sx_next];
        uint32_t c10 = row1[sx];
        uint32_t c11 = row1[sx_next];
        // R
        uint8_t r0 = fast_lerp((c00) & 0xFF, (c01) & 0xFF, xw);
        uint8_t r1 = fast_lerp((c10) & 0xFF, (c11) & 0xFF, xw);
        uint8_t r  = fast_lerp(r0, r1, y_weight);
        // G
        uint8_t g0 = fast_lerp((c00 >> 8) & 0xFF, (c01 >> 8) & 0xFF, xw);
        uint8_t g1 = fast_lerp((c10 >> 8) & 0xFF, (c11 >> 8) & 0xFF, xw);
        uint8_t g  = fast_lerp(g0, g1, y_weight);
        // B
        uint8_t b0 = fast_lerp((c00 >> 16) & 0xFF, (c01 >> 16) & 0xFF, xw);
        uint8_t b1 = fast_lerp((c10 >> 16) & 0xFF, (c11 >> 16) & 0xFF, xw);
        uint8_t b  = fast_lerp(b0, b1, y_weight);
        // A
        uint8_t a0 = fast_lerp((c00 >> 24) & 0xFF, (c01 >> 24) & 0xFF, xw);
        uint8_t a1 = fast_lerp((c10 >> 24) & 0xFF, (c11 >> 24) & 0xFF, xw);
        uint8_t a  = fast_lerp(a0, a1, y_weight);

        out_row_buffer[x] = (a << 24) | (b << 16) | (g << 8) | r;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_rbtsoft_tankfactory_miragetank_MirageTankCoder_encodeNative(
        JNIEnv *env, jobject,
        jobject bitmap1, jobject bitmap2, jobject outputBitmap,
        jfloat photo1K, jfloat photo2K, jint threshold) {

    AndroidBitmapInfo info1, info2, outInfo;
    if (AndroidBitmap_getInfo(env, bitmap1, &info1) < 0 ||
        AndroidBitmap_getInfo(env, bitmap2, &info2) < 0 ||
        AndroidBitmap_getInfo(env, outputBitmap, &outInfo) < 0) {
        return;
    }

    if (info1.format != ANDROID_BITMAP_FORMAT_RGBA_8888 ||
        info2.format != ANDROID_BITMAP_FORMAT_RGBA_8888 ||
        outInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return;
    }

    void *pixels1, *pixels2, *outputPixels;
    if (AndroidBitmap_lockPixels(env, bitmap1, &pixels1) < 0 ||
        AndroidBitmap_lockPixels(env, bitmap2, &pixels2) < 0 ||
        AndroidBitmap_lockPixels(env, outputBitmap, &outputPixels) < 0) {
        return;
    }
    float y_ratio1 = outInfo.height > 1 ? (float)(info1.height - 1) / (outInfo.height - 1) : 0;
    float y_ratio2 = outInfo.height > 1 ? (float)(info2.height - 1) / (outInfo.height - 1) : 0;
    ScaleXTable table1 = precompute_x_table(info1.width, outInfo.width);
    ScaleXTable table2 = precompute_x_table(info2.width, outInfo.width);
    const int32_t FIXED_SHIFT = 12;
    const int32_t k1_fixed = (int32_t)(photo1K * (1 << FIXED_SHIFT));
    const int32_t k2_fixed = (int32_t)(photo2K * (1 << FIXED_SHIFT));
    int16x8_t v_threshold = vdupq_n_s16((int16_t)threshold);
    int16x8_t v_255 = vdupq_n_s16(255);
    int16x8_t v_0 = vdupq_n_s16(0);
    uint32_t width = outInfo.width;
    uint32_t height = outInfo.height;

#pragma omp parallel for
    for (uint32_t y = 0; y < height; ++y) {
        std::vector<uint32_t> line1(width);
        std::vector<uint32_t> line2(width);
        get_scaled_row((uint8_t*)pixels1, info1.stride, info1.width, info1.height,
                       y, width, y_ratio1, table1, line1.data());
        get_scaled_row((uint8_t*)pixels2, info2.stride, info2.width, info2.height,
                       y, width, y_ratio2, table2, line2.data());
        auto* row1 = line1.data();
        auto* row2 = line2.data();
        auto* outRow = (uint32_t*)((char*)outputPixels + y * outInfo.stride);
        uint32_t x = 0;
        for (; x <= width - 8; x += 8) {
            uint8x8x4_t p1_vec = vld4_u8((uint8_t*)&row1[x]);
            uint8x8x4_t p2_vec = vld4_u8((uint8_t*)&row2[x]);
            uint8x8_t gray1_u8 = neon_to_gray(p1_vec.val[0], p1_vec.val[1], p1_vec.val[2]);
            uint8x8_t gray2_u8 = neon_to_gray(p2_vec.val[0], p2_vec.val[1], p2_vec.val[2]);
            int16x8_t g1_s16 = vreinterpretq_s16_u16(vmovl_u8(gray1_u8));
            int16x8_t g2_s16 = vreinterpretq_s16_u16(vmovl_u8(gray2_u8));
            int32x4_t v1_lo_32 = vmulq_n_s32(vmovl_s16(vget_low_s16(g1_s16)), k1_fixed);
            int32x4_t v1_hi_32 = vmulq_n_s32(vmovl_s16(vget_high_s16(g1_s16)), k1_fixed);
            int16x8_t v1_raw = vcombine_s16(
                    vshrn_n_s32(v1_lo_32, FIXED_SHIFT),
                    vshrn_n_s32(v1_hi_32, FIXED_SHIFT)
            );
            int16x8_t v1 = vmaxq_s16(v1_raw, v_threshold);
            v1 = vminq_s16(v1, v_255);
            int32x4_t v2_lo_32 = vmulq_n_s32(vmovl_s16(vget_low_s16(g2_s16)), k2_fixed);
            int32x4_t v2_hi_32 = vmulq_n_s32(vmovl_s16(vget_high_s16(g2_s16)), k2_fixed);
            int16x8_t v2_raw = vcombine_s16(
                    vshrn_n_s32(v2_lo_32, FIXED_SHIFT),
                    vshrn_n_s32(v2_hi_32, FIXED_SHIFT)
            );
            int16x8_t v2 = vmaxq_s16(v2_raw, v_0);
            v2 = vminq_s16(v2, v_threshold);
            int16x8_t diff = vsubq_s16(v1, v2);
            int16x8_t alpha_s16 = vsubq_s16(v_255, diff);
            uint8x8_t out_gray = vqmovun_s16(v2);
            uint8x8_t out_alpha = vqmovun_s16(alpha_s16);
            uint8x8x4_t out_vec;
            out_vec.val[0] = out_gray; // R
            out_vec.val[1] = out_gray; // G
            out_vec.val[2] = out_gray; // B
            out_vec.val[3] = out_alpha; // A
            vst4_u8((uint8_t*)&outRow[x], out_vec);
        }
        for (; x < width; ++x) {
            uint32_t pixel1 = row1[x];
            int r1 = (pixel1 >> 0) & 0xFF;
            int g1 = (pixel1 >> 8) & 0xFF;
            int b1 = (pixel1 >> 16) & 0xFF;
            uint8_t gray1 = toGrayScalar(r1, g1, b1);
            uint32_t pixel2 = row2[x];
            int r2 = (pixel2 >> 0) & 0xFF;
            int g2 = (pixel2 >> 8) & 0xFF;
            int b2 = (pixel2 >> 16) & 0xFF;
            uint8_t gray2 = toGrayScalar(r2, g2, b2);
            int v1 = std::min(std::max((int)(gray1 * photo1K), threshold), 255);
            int v2 = std::min(std::max((int)(gray2 * photo2K), 0), threshold);
            int alpha = 255 - (v1 - v2);
            int gray = v2;
            outRow[x] = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap1);
    AndroidBitmap_unlockPixels(env, bitmap2);
    AndroidBitmap_unlockPixels(env, outputBitmap);
}