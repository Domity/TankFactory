#include <jni.h>
#include <android/bitmap.h>
#include <algorithm>
#include <vector>
#include <omp.h>
#include <string>
#include <cmath>
#include <cstring>
#include <iostream>
#include <sstream>

int toGray(int r, int g, int b) {
    int gray = (r * 19595 + g * 38469 + b * 7472) >> 16;
    return static_cast<uint8_t>(std::min(255, gray));
}

extern "C" JNIEXPORT void JNICALL
Java_com_rbtsoft_tankfactory_miragetank_NativeBitmapProcessor_encodeBitmaps(
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

jbyteArray bitmapToByteArray(JNIEnv* env, jobject bitmap) {
    jclass bitmapClass = nullptr;
    jclass compressFormatClass = nullptr;
    jobject pngFormat = nullptr;
    jclass byteArrayOutputStreamClass = nullptr;
    jobject byteArrayOutputStream = nullptr;
    jbyteArray byteArray = nullptr;
    jobject resultByteArray = nullptr;

    do {
        bitmapClass = env->FindClass("android/graphics/Bitmap");
        if (bitmapClass == nullptr || env->ExceptionCheck()) break;

        jmethodID compressMethod = env->GetMethodID(bitmapClass, "compress", "(Landroid/graphics/Bitmap$CompressFormat;ILjava/io/OutputStream;)Z");
        if (compressMethod == nullptr || env->ExceptionCheck()) break;

        compressFormatClass = env->FindClass("android/graphics/Bitmap$CompressFormat");
        if (compressFormatClass == nullptr || env->ExceptionCheck()) break;

        jfieldID pngField = env->GetStaticFieldID(compressFormatClass, "PNG", "Landroid/graphics/Bitmap$CompressFormat;");
        if (pngField == nullptr || env->ExceptionCheck()) break;

        pngFormat = env->GetStaticObjectField(compressFormatClass, pngField);
        if (pngFormat == nullptr || env->ExceptionCheck()) break;

        byteArrayOutputStreamClass = env->FindClass("java/io/ByteArrayOutputStream");
        if (byteArrayOutputStreamClass == nullptr || env->ExceptionCheck()) break;

        jmethodID byteArrayOutputStreamConstructor = env->GetMethodID(byteArrayOutputStreamClass, "<init>", "()V");
        if (byteArrayOutputStreamConstructor == nullptr || env->ExceptionCheck()) break;

        byteArrayOutputStream = env->NewObject(byteArrayOutputStreamClass, byteArrayOutputStreamConstructor);
        if (byteArrayOutputStream == nullptr || env->ExceptionCheck()) break;

        env->CallBooleanMethod(bitmap, compressMethod, pngFormat, 100, byteArrayOutputStream);
        if (env->ExceptionCheck()) break;

        jmethodID toByteArrayMethod = env->GetMethodID(byteArrayOutputStreamClass, "toByteArray", "()[B");
        if (toByteArrayMethod == nullptr || env->ExceptionCheck()) break;

        byteArray = (jbyteArray)env->CallObjectMethod(byteArrayOutputStream, toByteArrayMethod);
        if (byteArray == nullptr || env->ExceptionCheck()) break;

        resultByteArray = byteArray;
        byteArray = nullptr;

    } while(false);

    env->DeleteLocalRef(bitmapClass);
    env->DeleteLocalRef(compressFormatClass);
    env->DeleteLocalRef(pngFormat);
    env->DeleteLocalRef(byteArrayOutputStreamClass);
    env->DeleteLocalRef(byteArrayOutputStream);
    env->DeleteLocalRef(byteArray);

    return (jbyteArray)resultByteArray;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_rbtsoft_tankfactory_lsbtank_LsbTankCoder_encodeNative(JNIEnv *env, jobject, jobject sur_pic, jobject ins_pic, jint compress) {
    if (compress == 0 || compress >= 8) return nullptr;

    const int lsb_mask[] = {0x1, 0x3, 0x7, 0xF, 0x1F, 0x3F, 0x7F};
    const char* signature_chars = "/By:f_Endman";
    std::vector<uint8_t> signature(signature_chars, signature_chars + strlen(signature_chars));

    jbyteArray ins_pic_byte_array = nullptr;
    jclass bitmap_class = nullptr;
    jobject tank_pic = nullptr;
    jbyte* ins_pic_bytes_ptr = nullptr;
    void* tank_pixels_ptr = nullptr;
    jclass bitmap_config_class = nullptr;
    jobject argb_8888_config = nullptr;
    jobject output_bitmap = nullptr;
    void* output_pixels_ptr = nullptr;
    jobject result_bitmap = nullptr;

    do {
        ins_pic_byte_array = bitmapToByteArray(env, ins_pic);
        if (ins_pic_byte_array == nullptr) break;

        jsize ins_pic_length = env->GetArrayLength(ins_pic_byte_array);
        long byte_for_lsb = (long)ins_pic_length * 8 / compress;

        AndroidBitmapInfo sur_pic_info;
        if (AndroidBitmap_getInfo(env, sur_pic, &sur_pic_info) < 0) break;

        long current_sur_pic_byte = (long)sur_pic_info.width * sur_pic_info.height * 3;

        double zoom = (double)byte_for_lsb / (double)current_sur_pic_byte * (compress >= 6 ? 1.05 : 1.01);
        double square_root_zoom = sqrt(zoom);

        int scaled_width = (int)(sur_pic_info.width * square_root_zoom);
        int scaled_height = (int)(sur_pic_info.height * square_root_zoom);

        bitmap_class = env->FindClass("android/graphics/Bitmap");
        if (bitmap_class == nullptr || env->ExceptionCheck()) break;

        jmethodID create_scaled_bitmap_method = env->GetStaticMethodID(bitmap_class, "createScaledBitmap", "(Landroid/graphics/Bitmap;IIZ)Landroid/graphics/Bitmap;");
        if (create_scaled_bitmap_method == nullptr || env->ExceptionCheck()) break;

        tank_pic = env->CallStaticObjectMethod(bitmap_class, create_scaled_bitmap_method, sur_pic, scaled_width, scaled_height, true);
        if (tank_pic == nullptr || env->ExceptionCheck()) break;

        std::string ins_pic_len_str = std::to_string(ins_pic_length);
        std::vector<uint8_t> header;
        header.insert(header.end(), ins_pic_len_str.begin(), ins_pic_len_str.end());
        header.push_back(0x01);
        const char* hidden_png = "hidden.png";
        header.insert(header.end(), hidden_png, hidden_png + strlen(hidden_png));
        header.push_back(0x01);
        const char* image_png = "image/png";
        header.insert(header.end(), image_png, image_png + strlen(image_png));
        header.push_back(0x00);

        ins_pic_bytes_ptr = env->GetByteArrayElements(ins_pic_byte_array, nullptr);
        if (ins_pic_bytes_ptr == nullptr) break;

        std::vector<uint8_t> data_to_hide = header;
        data_to_hide.insert(data_to_hide.end(), ins_pic_bytes_ptr, ins_pic_bytes_ptr + ins_pic_length);
        env->ReleaseByteArrayElements(ins_pic_byte_array, ins_pic_bytes_ptr, JNI_ABORT);
        ins_pic_bytes_ptr = nullptr;

        AndroidBitmapInfo tank_pic_info;
        if (AndroidBitmap_getInfo(env, tank_pic, &tank_pic_info) < 0) break;

        if (tank_pic_info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) break;

        if (AndroidBitmap_lockPixels(env, tank_pic, &tank_pixels_ptr) < 0) break;

        uint32_t* tank_pixels = (uint32_t*)tank_pixels_ptr;
        std::vector<uint8_t> tank_byte_array(tank_pic_info.width * tank_pic_info.height * 3);

        for (int i = 0; i < tank_pic_info.width * tank_pic_info.height; ++i) {
            uint32_t pixel = tank_pixels[i];
            tank_byte_array[i * 3 + 0] = (pixel >> 0) & 0xFF;  // R
            tank_byte_array[i * 3 + 1] = (pixel >> 8) & 0xFF;  // G
            tank_byte_array[i * 3 + 2] = (pixel >> 16) & 0xFF; // B
        }
        AndroidBitmap_unlockPixels(env, tank_pic);
        tank_pixels_ptr = nullptr;

        tank_byte_array[0] = (tank_byte_array[0] & 0xF8) | 0x00;
        tank_byte_array[1] = (tank_byte_array[1] & 0xF8) | 0x03;
        tank_byte_array[2] = (tank_byte_array[2] & 0xF8) | (compress & 0x7);

        int count = 0;
        int sn_count = 0;
        long long fifo = 0;
        int fifo_count = 0;
        for (size_t i = 3; i < tank_byte_array.size(); ++i) {
            if (fifo_count < compress) {
                uint8_t byte_to_hide;
                if (count < data_to_hide.size()) {
                    byte_to_hide = data_to_hide[count++];
                } else {
                    byte_to_hide = signature[sn_count++ % signature.size()];
                }
                long long unsigned_byte_long = (long long)(byte_to_hide & 0xFF);
                fifo = fifo | (unsigned_byte_long << (24 - fifo_count));
                fifo_count += 8;
            }
            int lsb_bits = (int)((fifo >> (32 - compress)) & lsb_mask[compress - 1]);
            tank_byte_array[i] = (tank_byte_array[i] & ~lsb_mask[compress - 1]) | lsb_bits;
            fifo = fifo << compress;
            fifo_count -= compress;
        }

        bitmap_config_class = env->FindClass("android/graphics/Bitmap$Config");
        if (bitmap_config_class == nullptr || env->ExceptionCheck()) break;

        jfieldID argb_8888_field = env->GetStaticFieldID(bitmap_config_class, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");
        if (argb_8888_field == nullptr || env->ExceptionCheck()) break;

        argb_8888_config = env->GetStaticObjectField(bitmap_config_class, argb_8888_field);
        if (argb_8888_config == nullptr || env->ExceptionCheck()) break;

        jmethodID create_bitmap_method = env->GetStaticMethodID(bitmap_class, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
        if (create_bitmap_method == nullptr || env->ExceptionCheck()) break;

        output_bitmap = env->CallStaticObjectMethod(bitmap_class, create_bitmap_method, tank_pic_info.width, tank_pic_info.height, argb_8888_config);
        if (output_bitmap == nullptr || env->ExceptionCheck()) break;

        if (AndroidBitmap_lockPixels(env, output_bitmap, &output_pixels_ptr) < 0) break;

        uint32_t* output_pixels = (uint32_t*)output_pixels_ptr;
        int output_index = 0;
        for (int i = 0; i < tank_pic_info.width * tank_pic_info.height; i++) {
            uint8_t red = tank_byte_array[output_index++];
            uint8_t green = tank_byte_array[output_index++];
            uint8_t blue = tank_byte_array[output_index++];
            output_pixels[i] = (0xFF << 24) | (blue << 16) | (green << 8) | red;
        }
        AndroidBitmap_unlockPixels(env, output_bitmap);
        output_pixels_ptr = nullptr;

        result_bitmap = output_bitmap;
        output_bitmap = nullptr;

    } while(false);

    if (ins_pic_bytes_ptr != nullptr) env->ReleaseByteArrayElements(ins_pic_byte_array, ins_pic_bytes_ptr, JNI_ABORT);
    if (tank_pixels_ptr != nullptr) AndroidBitmap_unlockPixels(env, tank_pic);
    if (output_pixels_ptr != nullptr) AndroidBitmap_unlockPixels(env, output_bitmap);

    env->DeleteLocalRef(ins_pic_byte_array);
    env->DeleteLocalRef(bitmap_class);
    env->DeleteLocalRef(tank_pic);
    env->DeleteLocalRef(bitmap_config_class);
    env->DeleteLocalRef(argb_8888_config);
    env->DeleteLocalRef(output_bitmap);
    return result_bitmap;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_rbtsoft_tankfactory_lsbtank_LsbTankCoder_decodeNative(JNIEnv *env, jobject, jobject tank_pic) {
    void* tank_pixels_ptr = nullptr;
    jbyteArray lsb_data = nullptr;
    jclass bitmap_factory_class = nullptr;
    jobject result_bitmap = nullptr;

    do {
        AndroidBitmapInfo tank_pic_info;
        if (AndroidBitmap_getInfo(env, tank_pic, &tank_pic_info) < 0) {
            break;
        }

        if (tank_pic_info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
            break;
        }

        if (AndroidBitmap_lockPixels(env, tank_pic, &tank_pixels_ptr) < 0) {
            break;
        }

        uint32_t* tank_pixels = (uint32_t*)tank_pixels_ptr;
        std::vector<uint8_t> tank_byte_array(tank_pic_info.width * tank_pic_info.height * 3);

        for (int i = 0; i < tank_pic_info.width * tank_pic_info.height; ++i) {
            uint32_t pixel = tank_pixels[i];
            tank_byte_array[i * 3 + 0] = (pixel >> 0) & 0xFF;  // R
            tank_byte_array[i * 3 + 1] = (pixel >> 8) & 0xFF;  // G
            tank_byte_array[i * 3 + 2] = (pixel >> 16) & 0xFF; // B
        }
        AndroidBitmap_unlockPixels(env, tank_pic);
        tank_pixels_ptr = nullptr;

        uint8_t byte0 = tank_byte_array[0];
        uint8_t byte1 = tank_byte_array[1];
        uint8_t byte2 = tank_byte_array[2];

        if ((byte0 & 0x7) != 0x0 || (byte1 & 0x7) != 0x3 || (byte2 & 0x7) == 0) {
            break;
        }

        int lsb_compress = byte2 & 0x7;
        const int lsb_mask[] = {0x1, 0x3, 0x7, 0xF, 0x1F, 0x3F, 0x7F};
        int current_lsb_mask = lsb_mask[lsb_compress - 1];

        long long fifo = 0;
        int fifo_count = 0;
        std::vector<uint8_t> lsb_byte_list;

        for (size_t i = 2; i < tank_byte_array.size(); ++i) {
            uint8_t current_byte = tank_byte_array[i];
            int new_lsb = current_byte & current_lsb_mask;
            fifo = fifo | (long long)new_lsb;
            if (fifo_count >= 8) {
                int shift_amount = fifo_count - 8;
                uint8_t decoded_byte = (uint8_t)(((unsigned long long)fifo >> shift_amount) & 0xFF);
                lsb_byte_list.push_back(decoded_byte);
                fifo_count -= 8;
            }
            fifo = fifo << lsb_compress;
            fifo_count += lsb_compress;
        }

        if (lsb_byte_list.size() < 256) {
            break;
        }

        int offset = 0;
        std::string s_lsb_count_builder;
        std::vector<uint8_t> lsb_file_name_list;
        std::string lsb_file_mime_builder;

        while (offset < lsb_byte_list.size() && offset < 0xFF && lsb_byte_list[offset] != 0x01) {
            uint8_t current_byte = lsb_byte_list[offset];
            if (current_byte >= '0' && current_byte <= '9') {
                s_lsb_count_builder += (char)current_byte;
            } else {
                break;
            }
            offset++;
        }
        if (offset == lsb_byte_list.size() || offset == 0xFF) break;

        offset++;
        while (offset < lsb_byte_list.size() && offset < 0xFF && lsb_byte_list[offset] != 0x01) {
            lsb_file_name_list.push_back(lsb_byte_list[offset]);
            offset++;
        }
        if (offset == lsb_byte_list.size() || offset == 0xFF) break;

        offset++;
        while (offset < lsb_byte_list.size() && offset < 0xFF && lsb_byte_list[offset] != 0x00) {
            lsb_file_mime_builder += (char)lsb_byte_list[offset];
            offset++;
        }
        if (offset == lsb_byte_list.size() || offset == 0xFF) break;
        offset++;

        int lsb_count = 0;
        try {
            lsb_count = std::stoi(s_lsb_count_builder);
        } catch (const std::exception& e) {
            break;
        }

        if (lsb_byte_list.size() < offset + lsb_count) {
            break;
        }

        lsb_data = env->NewByteArray(lsb_count);
        if (lsb_data == nullptr) break;

        env->SetByteArrayRegion(lsb_data, 0, lsb_count, (jbyte*)(lsb_byte_list.data() + offset));

        bitmap_factory_class = env->FindClass("android/graphics/BitmapFactory");
        if (bitmap_factory_class == nullptr || env->ExceptionCheck()) break;

        jmethodID decode_byte_array_method = env->GetStaticMethodID(bitmap_factory_class, "decodeByteArray", "([BII)Landroid/graphics/Bitmap;");
        if (decode_byte_array_method == nullptr || env->ExceptionCheck()) break;

        result_bitmap = env->CallStaticObjectMethod(bitmap_factory_class, decode_byte_array_method, lsb_data, 0, lsb_count);

    } while(false);

    if (tank_pixels_ptr != nullptr) AndroidBitmap_unlockPixels(env, tank_pic);
    env->DeleteLocalRef(lsb_data);
    env->DeleteLocalRef(bitmap_factory_class);

    return result_bitmap;
}