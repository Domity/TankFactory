#include <jni.h>
#include <android/bitmap.h>
#include <cmath>
#include <cstring>
#include <cstdio>
#include <cstdlib>

struct LsbBitStream {
    uint32_t fifo = 0;
    int fifo_count = 0;

    const uint8_t* data{};
    size_t data_size{};
    size_t data_idx = 0;

    const uint8_t* sig{};
    size_t sig_len{};
    size_t sig_idx = 0;

    int compress{};
    int shift_complement{};
    uint32_t lsb_mask{};
    uint32_t clean_mask{};

    __attribute__((always_inline))
    inline uint32_t process_color(uint32_t color_byte) {
        if (fifo_count < compress) {
            uint8_t byte_to_hide;
            if (__builtin_expect(data_idx < data_size, 1)) {
                byte_to_hide = data[data_idx++];
            } else {
                byte_to_hide = sig[sig_idx++];
                if (__builtin_expect(sig_idx == sig_len, 0)) {
                    sig_idx = 0;
                }
            }
            fifo |= ((uint32_t)byte_to_hide) << (24 - fifo_count);
            fifo_count += 8;
        }

        uint32_t lsb_bits = (fifo >> shift_complement) & lsb_mask;
        fifo <<= compress;
        fifo_count -= compress;

        return (color_byte & clean_mask) | lsb_bits;
    }
};

jbyteArray bitmapToByteArray(JNIEnv* env, jobject bitmap);

extern "C" JNIEXPORT jobject JNICALL
Java_com_rbtsoft_tankfactory_lsbtank_LsbTankCoder_encodeNative(JNIEnv *env, jobject, jobject sur_pic, jobject ins_pic, jint compress) {
    if (compress <= 0 || compress >= 8) return nullptr;

    const char* signature = "/By:f_Endman";
    const size_t sig_len = strlen(signature);

    jbyteArray ins_pic_byte_array = nullptr;
    jclass bitmap_class = nullptr;
    jobject tank_pic = nullptr;
    jbyte* ins_pic_bytes_ptr = nullptr;
    void* tank_pixels_ptr = nullptr;
    jobject result_bitmap = nullptr;
    uint8_t* raw_data = nullptr;

    do {
        ins_pic_byte_array = bitmapToByteArray(env, ins_pic);
        if (ins_pic_byte_array == nullptr) break;
        jsize ins_pic_length = env->GetArrayLength(ins_pic_byte_array);

        AndroidBitmapInfo sur_pic_info;
        if (AndroidBitmap_getInfo(env, sur_pic, &sur_pic_info) < 0) break;

        long byte_for_lsb = (long)ins_pic_length * 8 / compress;
        long current_sur_pic_byte = (long)sur_pic_info.width * sur_pic_info.height * 3;

        float zoom = (float)byte_for_lsb / (float)current_sur_pic_byte * (compress >= 6 ? 1.05f : 1.01f);
        float square_root_zoom = sqrtf(zoom);
        int scaled_width = (int)(sur_pic_info.width * square_root_zoom);
        int scaled_height = (int)(sur_pic_info.height * square_root_zoom);

        bitmap_class = env->FindClass("android/graphics/Bitmap");
        jmethodID create_scaled_bitmap_method = env->GetStaticMethodID(bitmap_class, "createScaledBitmap", "(Landroid/graphics/Bitmap;IIZ)Landroid/graphics/Bitmap;");
        tank_pic = env->CallStaticObjectMethod(bitmap_class, create_scaled_bitmap_method, sur_pic, scaled_width, scaled_height, true);
        if (tank_pic == nullptr || env->ExceptionCheck()) break;

        char header_buf[128];
        int header_len = snprintf(header_buf, sizeof(header_buf), "%d\x01hidden.webp\x01image/webp", ins_pic_length);
        int total_header_size = header_len + 1;

        size_t total_data_size = total_header_size + ins_pic_length;
        raw_data = (uint8_t*)malloc(total_data_size);
        if (!raw_data) break;

        memcpy(raw_data, header_buf, total_header_size);

        ins_pic_bytes_ptr = env->GetByteArrayElements(ins_pic_byte_array, nullptr);
        if (ins_pic_bytes_ptr == nullptr) break;
        memcpy(raw_data + total_header_size, ins_pic_bytes_ptr, ins_pic_length);

        env->ReleaseByteArrayElements(ins_pic_byte_array, ins_pic_bytes_ptr, JNI_ABORT);
        ins_pic_bytes_ptr = nullptr;

        AndroidBitmapInfo tank_pic_info;
        if (AndroidBitmap_getInfo(env, tank_pic, &tank_pic_info) < 0) break;
        if (AndroidBitmap_lockPixels(env, tank_pic, &tank_pixels_ptr) < 0) break;

        uint32_t* pixels = (uint32_t*)tank_pixels_ptr;
        int total_pixels = tank_pic_info.width * tank_pic_info.height;

        LsbBitStream stream;
        stream.data = raw_data;
        stream.data_size = total_data_size;
        stream.sig = (const uint8_t*)signature;
        stream.sig_len = sig_len;
        stream.compress = compress;
        stream.shift_complement = 32 - compress;
        stream.lsb_mask = (1 << compress) - 1;
        stream.clean_mask = ~stream.lsb_mask;

        if (total_pixels > 0) {
            uint32_t p = pixels[0];
            uint32_t r = (p & 0xFF) & 0xF8 | 0x00;
            uint32_t g = ((p >> 8) & 0xFF) & 0xF8 | 0x03;
            uint32_t b = ((p >> 16) & 0xFF) & 0xF8 | (compress & 0x7);
            pixels[0] = (0xFFU << 24) | (b << 16) | (g << 8) | r;
        }
        for (int i = 1; i < total_pixels; ++i) {
            uint32_t p = pixels[i];
            uint32_t r = stream.process_color(p & 0xFF);
            uint32_t g = stream.process_color((p >> 8) & 0xFF);
            uint32_t b = stream.process_color((p >> 16) & 0xFF);
            pixels[i] = (0xFFU << 24) | (b << 16) | (g << 8) | r;
        }

        AndroidBitmap_unlockPixels(env, tank_pic);
        tank_pixels_ptr = nullptr;
        result_bitmap = tank_pic;

    } while(false);

    if (raw_data) free(raw_data);
    if (tank_pixels_ptr != nullptr) AndroidBitmap_unlockPixels(env, tank_pic);
    if (ins_pic_byte_array != nullptr) env->DeleteLocalRef(ins_pic_byte_array);
    if (bitmap_class != nullptr) env->DeleteLocalRef(bitmap_class);

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