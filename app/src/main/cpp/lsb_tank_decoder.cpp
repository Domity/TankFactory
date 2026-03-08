#include <jni.h>
#include <android/bitmap.h>

#define PROCESS_EXTRACTED_BYTE(b) \
    if (__builtin_expect(is_data_phase, 1)) { \
        data_ptr[written++] = (b); \
        if (__builtin_expect(written >= final_data_size, 0)) goto decode_finish; \
    } else { \
        if (header_state == 0) { \
            if ((b) == 0x01) { \
                header_state = 1; \
                if (final_data_size == 0 || final_data_size > (size_t)(total_pixels * 3)) goto decode_error; \
                lsb_data = env->NewByteArray((jsize)final_data_size); \
                if (!lsb_data) goto decode_error; \
                data_ptr = (uint8_t*)env->GetPrimitiveArrayCritical(lsb_data, nullptr); \
            } else if ((b) >= '0' && (b) <= '9') { \
               final_data_size = final_data_size * 10 + ((b) - '0'); \
            } else goto decode_error; \
        } else if (header_state == 1) { \
            if ((b) == 0x01) header_state = 2; \
        } else if (header_state == 2) { \
            if ((b) == 0x00) is_data_phase = true; \
        } \
    }

extern "C" JNIEXPORT jobject JNICALL
Java_com_rbtsoft_tankfactory_lsbtank_LsbTankCoder_decodeNative(JNIEnv *env, jobject, jobject tank_pic) {
    void* tank_pixels_ptr = nullptr;
    jbyteArray lsb_data = nullptr;
    uint8_t* data_ptr = nullptr;
    jobject result_bitmap = nullptr;

    AndroidBitmapInfo tank_pic_info;
    if (AndroidBitmap_getInfo(env, tank_pic, &tank_pic_info) < 0) return nullptr;
    if (tank_pic_info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return nullptr;
    if (AndroidBitmap_lockPixels(env, tank_pic, &tank_pixels_ptr) < 0) return nullptr;

    auto* pixels = (uint32_t*)tank_pixels_ptr;
    int total_pixels = tank_pic_info.width * tank_pic_info.height;
    if (total_pixels <= 0) goto decode_error;

    {
        uint32_t p0 = pixels[0];
        uint32_t r0 = p0 & 0xFF;
        uint32_t g0 = (p0 >> 8) & 0xFF;
        uint32_t b0 = (p0 >> 16) & 0xFF;
        if ((r0 & 0x7) != 0x0 || (g0 & 0x7) != 0x3 || (b0 & 0x7) == 0) {
            goto decode_error;
        }
    }

    {
        uint32_t lsb_compress = (pixels[0] >> 16) & 0x7;
        uint32_t lsb_mask = (1 << lsb_compress) - 1;
        uint32_t fifo = 0;
        int fifo_count = 0;

        int header_state = 0;
        bool is_data_phase = false;
        size_t final_data_size = 0;
        size_t written = 0;

        for (int i = 1; i < total_pixels; ++i) {
            uint32_t p = pixels[i];

            fifo = (fifo << lsb_compress) | (p & lsb_mask); // R
            fifo_count += lsb_compress;
            if (fifo_count >= 8) {
                fifo_count -= 8;
                uint8_t byte_val = (fifo >> fifo_count) & 0xFF;
                PROCESS_EXTRACTED_BYTE(byte_val);
            }

            fifo = (fifo << lsb_compress) | ((p >> 8) & lsb_mask); // G
            fifo_count += lsb_compress;
            if (fifo_count >= 8) {
                fifo_count -= 8;
                uint8_t byte_val = (fifo >> fifo_count) & 0xFF;
                PROCESS_EXTRACTED_BYTE(byte_val);
            }

            fifo = (fifo << lsb_compress) | ((p >> 16) & lsb_mask); // B
            fifo_count += lsb_compress;
            if (fifo_count >= 8) {
                fifo_count -= 8;
                uint8_t byte_val = (fifo >> fifo_count) & 0xFF;
                PROCESS_EXTRACTED_BYTE(byte_val);
            }
        }
    }

    decode_finish:
    if (data_ptr) env->ReleasePrimitiveArrayCritical(lsb_data, data_ptr, 0);
    AndroidBitmap_unlockPixels(env, tank_pic);

    if (lsb_data != nullptr) {
        jclass bitmap_factory_class = env->FindClass("android/graphics/BitmapFactory");
        if (bitmap_factory_class != nullptr) {
            jmethodID decode_method = env->GetStaticMethodID(bitmap_factory_class, "decodeByteArray", "([BII)Landroid/graphics/Bitmap;");
            if (decode_method != nullptr) {
                result_bitmap = env->CallStaticObjectMethod(bitmap_factory_class, decode_method, lsb_data, 0, env->GetArrayLength(lsb_data));
            }
            env->DeleteLocalRef(bitmap_factory_class);
        }
        env->DeleteLocalRef(lsb_data);
    }
    return result_bitmap;

    decode_error:
    if (data_ptr) env->ReleasePrimitiveArrayCritical(lsb_data, data_ptr, JNI_ABORT);
    AndroidBitmap_unlockPixels(env, tank_pic);
    if (lsb_data) env->DeleteLocalRef(lsb_data);
    return nullptr;
}