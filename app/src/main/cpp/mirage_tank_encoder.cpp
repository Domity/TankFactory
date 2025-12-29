#include <jni.h>
#include <android/bitmap.h>
#include <algorithm>
#include <arm_neon.h>
#include <omp.h>

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
        info1.width != info2.width || info1.height != info2.height ||
        info1.width != outInfo.width || info1.height != outInfo.height) {
        return;
    }

    void *pixels1, *pixels2, *outputPixels;
    if (AndroidBitmap_lockPixels(env, bitmap1, &pixels1) < 0 ||
        AndroidBitmap_lockPixels(env, bitmap2, &pixels2) < 0 ||
        AndroidBitmap_lockPixels(env, outputBitmap, &outputPixels) < 0) {
        return;
    }

    uint32_t width = info1.width;
    uint32_t height = info1.height;

    const int32_t FIXED_SHIFT = 12;
    const int32_t k1_fixed = (int32_t)(photo1K * (1 << FIXED_SHIFT));
    const int32_t k2_fixed = (int32_t)(photo2K * (1 << FIXED_SHIFT));

    int16x8_t v_threshold = vdupq_n_s16((int16_t)threshold);
    int16x8_t v_255 = vdupq_n_s16(255);
    int16x8_t v_0 = vdupq_n_s16(0);

#pragma omp parallel for
    for (uint32_t y = 0; y < height; ++y) {
        auto* row1 = (uint32_t*)((char*)pixels1 + y * info1.stride);
        auto* row2 = (uint32_t*)((char*)pixels2 + y * info2.stride);
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