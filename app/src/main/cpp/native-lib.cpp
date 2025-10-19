#include <jni.h>
#include <android/bitmap.h>
#include <algorithm>
#include <vector>

int toGray(int r, int g, int b) {
    return static_cast<int>(r * 0.299f + g * 0.587f + b * 0.114f);
}

extern "C" JNIEXPORT void JNICALL
Java_com_rbtsoft_tankfactory_MirageTank_NativeBitmapProcessor_encodeBitmaps(
        JNIEnv *env, jobject,
        jobject bitmap1, jobject bitmap2, jobject outputBitmap,
        jfloat photo1K, jfloat photo2K, jint threshold,
        jint startY, jint endY) {

    AndroidBitmapInfo info1, info2, outInfo;
    if (AndroidBitmap_getInfo(env, bitmap1, &info1) < 0 ||
        AndroidBitmap_getInfo(env, bitmap2, &info2) < 0 ||
        AndroidBitmap_getInfo(env, outputBitmap, &outInfo) < 0) {
        return;
    }

    if (info1.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || info1.width != info2.width || info1.height != info2.height || info1.width != outInfo.width || info1.height != outInfo.height) {
        return;
    }

    void *pixels1, *pixels2, *outputPixels;
    if (AndroidBitmap_lockPixels(env, bitmap1, &pixels1) < 0 ||
        AndroidBitmap_lockPixels(env, bitmap2, &pixels2) < 0 ||
        AndroidBitmap_lockPixels(env, outputBitmap, &outputPixels) < 0) {
        return;
    }

    uint32_t width = info1.width;

    for (uint32_t y = startY; y < endY; ++y) {
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

            int v1 = std::min(std::max((int)(gray1 * photo1K), threshold + 1), 254);
            int v2 = std::min(std::max((int)(gray2 * photo2K), 1), threshold);
            int alpha = 255 - (v1 - v2);
            int safeAlpha = (alpha == 0) ? 1 : alpha;
            int gray = std::min(std::max((int)(255.0f * v2 / safeAlpha), 0), 255);
            int pGray = (gray * alpha) / 255;
            outRow[x] = (alpha << 24) | (pGray << 16) | (pGray << 8) | pGray;
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap1);
    AndroidBitmap_unlockPixels(env, bitmap2);
    AndroidBitmap_unlockPixels(env, outputBitmap);
}