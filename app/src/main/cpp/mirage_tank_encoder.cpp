#include <jni.h>
#include <android/bitmap.h>
#include <algorithm>

int toGray(int r, int g, int b) {
    int gray = (r * 19595 + g * 38469 + b * 7472) >> 16;
    return static_cast<uint8_t>(std::min(255, gray));
}

extern "C" JNIEXPORT void JNICALL
Java_com_rbtsoft_tankfactory_miragetank_MiragetankCoder_encodeNative(
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

    #pragma omp parallel for
    for (uint32_t y = 0; y < height; ++y) {
        auto* row1 = (uint32_t*)((char*)pixels1 + y * info1.stride);
        auto* row2 = (uint32_t*)((char*)pixels2 + y * info2.stride);
        auto* outRow = (uint32_t*)((char*)outputPixels + y * outInfo.stride);

        for (uint32_t x = 0; x < width; ++x) {

            uint32_t pixel1 = row1[x];
            int r1 = (pixel1 >> 0) & 0xFF;
            int g1 = (pixel1 >> 8) & 0xFF;
            int b1 = (pixel1 >> 16) & 0xFF;
            uint8_t gray1 = toGray(r1, g1, b1);

            uint32_t pixel2 = row2[x];
            int r2 = (pixel2 >> 0) & 0xFF;
            int g2 = (pixel2 >> 8) & 0xFF;
            int b2 = (pixel2 >> 16) & 0xFF;
            uint8_t gray2 = toGray(r2, g2, b2);

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
