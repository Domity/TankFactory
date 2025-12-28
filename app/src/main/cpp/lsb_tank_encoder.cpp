#include <jni.h>
#include <android/bitmap.h>
#include <vector>
#include <string>
#include <cmath>
#include <cstring>

#define PROCESS_BYTE(COLOR_BYTE) \
    if (fifo_count < compress) { \
        uint8_t byte_to_hide; \
        if (__builtin_expect(count < data_size, 1)) { \
            byte_to_hide = data_ptr[count++]; \
        } else { \
            byte_to_hide = sig_ptr[sn_count++ % sig_len]; \
        } \
        fifo |= ((long long)byte_to_hide) << (24 - fifo_count); \
        fifo_count += 8; \
    } \
    lsb_bits = (int)((fifo >> shift_complement) & lsb_mask_val); \
    COLOR_BYTE = (COLOR_BYTE & clean_mask_val) | lsb_bits; \
    fifo <<= compress; \
    fifo_count -= compress;

jbyteArray bitmapToByteArray(JNIEnv* env, jobject bitmap);

extern "C"
JNIEXPORT jobject JNICALL
Java_com_rbtsoft_tankfactory_lsbtank_LsbTankCoder_encodeNative(JNIEnv *env, jobject, jobject sur_pic, jobject ins_pic, jint compress) {
    if (compress == 0 || compress >= 8) return nullptr;
    const int lsb_mask_val = (1 << compress) - 1;
    const int clean_mask_val = ~lsb_mask_val;
    const int shift_complement = 32 - compress;
    const char* signature_chars = "/By:f_Endman";
    const size_t sig_len = strlen(signature_chars);
    std::vector<uint8_t> signature(signature_chars, signature_chars + sig_len);
    jbyteArray ins_pic_byte_array = nullptr;
    jclass bitmap_class = nullptr;
    jobject tank_pic = nullptr;
    jbyte* ins_pic_bytes_ptr = nullptr;
    void* tank_pixels_ptr = nullptr;
    jobject result_bitmap = nullptr;

    do {
        ins_pic_byte_array = bitmapToByteArray(env, ins_pic);
        if (ins_pic_byte_array == nullptr) break;
        jsize ins_pic_length = env->GetArrayLength(ins_pic_byte_array);
        AndroidBitmapInfo sur_pic_info;
        if (AndroidBitmap_getInfo(env, sur_pic, &sur_pic_info) < 0) break;
        long byte_for_lsb = (long)ins_pic_length * 8 / compress;
        long current_sur_pic_byte = (long)sur_pic_info.width * sur_pic_info.height * 3;
        double zoom = (double)byte_for_lsb / (double)current_sur_pic_byte * (compress >= 6 ? 1.05 : 1.01);
        double square_root_zoom = sqrt(zoom);
        int scaled_width = (int)(sur_pic_info.width * square_root_zoom);
        int scaled_height = (int)(sur_pic_info.height * square_root_zoom);

        bitmap_class = env->FindClass("android/graphics/Bitmap");
        jmethodID create_scaled_bitmap_method = env->GetStaticMethodID(bitmap_class, "createScaledBitmap", "(Landroid/graphics/Bitmap;IIZ)Landroid/graphics/Bitmap;");
        tank_pic = env->CallStaticObjectMethod(bitmap_class, create_scaled_bitmap_method, sur_pic, scaled_width, scaled_height, true);
        if (tank_pic == nullptr || env->ExceptionCheck()) break;
        std::string ins_pic_len_str = std::to_string(ins_pic_length);
        std::vector<uint8_t> data_to_hide;
        data_to_hide.reserve(40 + ins_pic_length);
        data_to_hide.insert(data_to_hide.end(), ins_pic_len_str.begin(), ins_pic_len_str.end());
        data_to_hide.push_back(0x01);

        const char* hidden_filename = "hidden.webp";
        data_to_hide.insert(data_to_hide.end(), hidden_filename, hidden_filename + strlen(hidden_filename));
        data_to_hide.push_back(0x01);

        const char* mime_type = "image/webp";
        data_to_hide.insert(data_to_hide.end(), mime_type, mime_type + strlen(mime_type));
        data_to_hide.push_back(0x00);

        ins_pic_bytes_ptr = env->GetByteArrayElements(ins_pic_byte_array, nullptr);
        if (ins_pic_bytes_ptr == nullptr) break;
        data_to_hide.insert(data_to_hide.end(), ins_pic_bytes_ptr, ins_pic_bytes_ptr + ins_pic_length);
        env->ReleaseByteArrayElements(ins_pic_byte_array, ins_pic_bytes_ptr, JNI_ABORT);
        ins_pic_bytes_ptr = nullptr;
        AndroidBitmapInfo tank_pic_info;
        if (AndroidBitmap_getInfo(env, tank_pic, &tank_pic_info) < 0) break;
        if (AndroidBitmap_lockPixels(env, tank_pic, &tank_pixels_ptr) < 0) break;
        uint32_t* pixels = (uint32_t*)tank_pixels_ptr;
        int total_pixels = tank_pic_info.width * tank_pic_info.height;
        size_t count = 0;
        size_t sn_count = 0;
        long long fifo = 0;
        int fifo_count = 0;
        int lsb_bits;
        const uint8_t* data_ptr = data_to_hide.data();
        size_t data_size = data_to_hide.size();
        const uint8_t* sig_ptr = signature.data();

        if (total_pixels > 0) {
            uint32_t p = pixels[0];
            uint8_t r = (p & 0xFF) & 0xF8 | 0x00;
            uint8_t g = ((p >> 8) & 0xFF) & 0xF8 | 0x03;
            uint8_t b = ((p >> 16) & 0xFF) & 0xF8 | (compress & 0x7);
            pixels[0] = (0xFFU << 24) | (b << 16) | (g << 8) | r;
        }

        for (int i = 1; i < total_pixels; ++i) {
            uint32_t p = pixels[i];
            uint8_t r = p & 0xFF;
            uint8_t g = (p >> 8) & 0xFF;
            uint8_t b = (p >> 16) & 0xFF;
            PROCESS_BYTE(r)
            PROCESS_BYTE(g)
            PROCESS_BYTE(b)
            pixels[i] = (0xFFU << 24) | (b << 16) | (g << 8) | r;
        }

        AndroidBitmap_unlockPixels(env, tank_pic);
        tank_pixels_ptr = nullptr;
        result_bitmap = tank_pic;

    } while(false);

    if (ins_pic_bytes_ptr != nullptr) env->ReleaseByteArrayElements(ins_pic_byte_array, ins_pic_bytes_ptr, JNI_ABORT);
    if (tank_pixels_ptr != nullptr) AndroidBitmap_unlockPixels(env, tank_pic);
    env->DeleteLocalRef(ins_pic_byte_array);
    env->DeleteLocalRef(bitmap_class);
    return result_bitmap;
}

jbyteArray bitmapToByteArray(JNIEnv* env, jobject bitmap) {
    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    if (bitmapClass == nullptr) return nullptr;
    jclass compressFormatClass = env->FindClass("android/graphics/Bitmap$CompressFormat");
    if (compressFormatClass == nullptr) return nullptr;
    jfieldID webpLosslessField = env->GetStaticFieldID(compressFormatClass, "WEBP_LOSSLESS", "Landroid/graphics/Bitmap$CompressFormat;");
    if (webpLosslessField == nullptr) return nullptr;
    jobject webpLossless = env->GetStaticObjectField(compressFormatClass, webpLosslessField);
    jclass byteArrayOutputStreamClass = env->FindClass("java/io/ByteArrayOutputStream");
    jmethodID byteArrayOutputStreamConstructor = env->GetMethodID(byteArrayOutputStreamClass, "<init>", "()V");
    jobject byteArrayOutputStream = env->NewObject(byteArrayOutputStreamClass, byteArrayOutputStreamConstructor);
    jmethodID compressMethod = env->GetMethodID(bitmapClass, "compress", "(Landroid/graphics/Bitmap$CompressFormat;ILjava/io/OutputStream;)Z");
    env->CallBooleanMethod(bitmap, compressMethod, webpLossless, 100, byteArrayOutputStream);
    jmethodID toByteArrayMethod = env->GetMethodID(byteArrayOutputStreamClass, "toByteArray", "()[B");
    jbyteArray byteArray = (jbyteArray)env->CallObjectMethod(byteArrayOutputStream, toByteArrayMethod);
    env->DeleteLocalRef(bitmapClass);
    env->DeleteLocalRef(compressFormatClass);
    env->DeleteLocalRef(webpLossless);
    env->DeleteLocalRef(byteArrayOutputStreamClass);
    env->DeleteLocalRef(byteArrayOutputStream);

    return byteArray;
}