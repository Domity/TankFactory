#include <jni.h>
#include <android/bitmap.h>
#include <algorithm>
#include <arm_neon.h>
#include <omp.h>

inline uint32_t get_pixel_at(const AndroidBitmapInfo& info, void* pixels, int x, int y) {
    x = std::max(0, std::min((int)info.width - 1, x));
    y = std::max(0, std::min((int)info.height - 1, y));
    return ((uint32_t*)((char*)pixels + y * info.stride))[x];
}

void scale_bitmap_bilinear(
    const AndroidBitmapInfo& srcInfo, void* srcPixels,
    const AndroidBitmapInfo& dstInfo, void* dstPixels) {

    float x_ratio = dstInfo.width > 1 ? (float)(srcInfo.width - 1) / (dstInfo.width - 1) : 0;
    float y_ratio = dstInfo.height > 1 ? (float)(srcInfo.height - 1) / (dstInfo.height - 1) : 0;

    for (uint32_t y = 0; y < dstInfo.height; ++y) {
        auto* dstRow = (uint32_t*)((char*)dstPixels + y * dstInfo.stride);
        float src_yf = y * y_ratio;
        int src_y = (int)src_yf;
        float y_diff = src_yf - src_y;

        for (uint32_t x = 0; x < dstInfo.width; ++x) {
            float src_xf = x * x_ratio;
            int src_x = (int)src_xf;
            float x_diff = src_xf - src_x;

            uint32_t p1 = get_pixel_at(srcInfo, srcPixels, src_x, src_y);
            uint32_t p2 = get_pixel_at(srcInfo, srcPixels, src_x + 1, src_y);
            uint32_t p3 = get_pixel_at(srcInfo, srcPixels, src_x, src_y + 1);
            uint32_t p4 = get_pixel_at(srcInfo, srcPixels, src_x + 1, src_y + 1);

            uint8_t r1 = (p1 >> 0) & 0xff, g1 = (p1 >> 8) & 0xff, b1 = (p1 >> 16) & 0xff, a1 = (p1 >> 24) & 0xff;
            uint8_t r2 = (p2 >> 0) & 0xff, g2 = (p2 >> 8) & 0xff, b2 = (p2 >> 16) & 0xff, a2 = (p2 >> 24) & 0xff;
            uint8_t r3 = (p3 >> 0) & 0xff, g3 = (p3 >> 8) & 0xff, b3 = (p3 >> 16) & 0xff, a3 = (p3 >> 24) & 0xff;
            uint8_t r4 = (p4 >> 0) & 0xff, g4 = (p4 >> 8) & 0xff, b4 = (p4 >> 16) & 0xff, a4 = (p4 >> 24) & 0xff;

            auto lerp = [](float t, uint8_t ca, uint8_t cb) {
                return (uint8_t)(ca * (1.0f - t) + cb * t);
            };

            uint8_t r = lerp(y_diff, lerp(x_diff, r1, r2), lerp(x_diff, r3, r4));
            uint8_t g = lerp(y_diff, lerp(x_diff, g1, g2), lerp(x_diff, g3, g4));
            uint8_t b = lerp(y_diff, lerp(x_diff, b1, b2), lerp(x_diff, b3, b4));
            uint8_t a = lerp(y_diff, lerp(x_diff, a1, a2), lerp(x_diff, a3, a4));

            dstRow[x] = (a << 24) | (b << 16) | (g << 8) | r;
        }
    }
}

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

    uint8x8_t gray = vqmovn_u16(vcombine_u16(res_lo, res_hi));
    return gray;
}

int toGrayScalar(int r, int g, int b) {
    int gray = (r * 19595 + g * 38469 + b * 7472) >> 16;
    return std::min(255, gray);
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

    void* finalPixels1 = pixels1;
    char* scaled_pixels1_buf = nullptr;
    if (info1.width != outInfo.width || info1.height != outInfo.height) {
        scaled_pixels1_buf = new char[outInfo.stride * outInfo.height];
        scale_bitmap_bilinear(info1, pixels1, outInfo, scaled_pixels1_buf);
        finalPixels1 = scaled_pixels1_buf;
    }

    void* finalPixels2 = pixels2;
    char* scaled_pixels2_buf = nullptr;
    if (info2.width != outInfo.width || info2.height != outInfo.height) {
        scaled_pixels2_buf = new char[outInfo.stride * outInfo.height];
        scale_bitmap_bilinear(info2, pixels2, outInfo, scaled_pixels2_buf);
        finalPixels2 = scaled_pixels2_buf;
    }

    uint32_t width = outInfo.width;
    uint32_t height = outInfo.height;

    const int32_t FIXED_SHIFT = 12;
    const int32_t k1_fixed = (int32_t)(photo1K * (1 << FIXED_SHIFT));
    const int32_t k2_fixed = (int32_t)(photo2K * (1 << FIXED_SHIFT));

    int16x8_t v_threshold = vdupq_n_s16((int16_t)threshold);
    int16x8_t v_255 = vdupq_n_s16(255);
    int16x8_t v_0 = vdupq_n_s16(0);

#pragma omp parallel for
    for (uint32_t y = 0; y < height; ++y) {
        auto* row1 = (uint32_t*)((char*)finalPixels1 + y * outInfo.stride);
        auto* row2 = (uint32_t*)((char*)finalPixels2 + y * outInfo.stride);
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

    delete[] scaled_pixels1_buf;
    delete[] scaled_pixels2_buf;

    AndroidBitmap_unlockPixels(env, bitmap1);
    AndroidBitmap_unlockPixels(env, bitmap2);
    AndroidBitmap_unlockPixels(env, outputBitmap);
}