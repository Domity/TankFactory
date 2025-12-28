#include <jni.h>
#include <android/bitmap.h>
#include <vector>
#include <string>
#include <stdexcept>
#include <cstring>

extern "C"
JNIEXPORT jobject JNICALL
Java_com_rbtsoft_tankfactory_lsbtank_LsbTankCoder_decodeNative(JNIEnv *env, jobject, jobject tank_pic) {
    void* tank_pixels_ptr = nullptr;
    jbyteArray lsb_data = nullptr;
    jclass bitmap_factory_class = nullptr;
    jobject result_bitmap = nullptr;
    const int STATE_READING_LENGTH = 0;
    const int STATE_READING_NAME = 1;
    const int STATE_READING_MIME = 2;
    const int STATE_READING_DATA = 3;

    do {
        AndroidBitmapInfo tank_pic_info;
        if (AndroidBitmap_getInfo(env, tank_pic, &tank_pic_info) < 0) break;
        if (tank_pic_info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) break;
        if (AndroidBitmap_lockPixels(env, tank_pic, &tank_pixels_ptr) < 0) break;

        uint32_t* pixels = (uint32_t*)tank_pixels_ptr;
        int total_pixels = tank_pic_info.width * tank_pic_info.height;
        if (total_pixels <= 0) break;
        uint32_t p0 = pixels[0];
        uint8_t r0 = p0 & 0xFF;
        uint8_t g0 = (p0 >> 8) & 0xFF;
        uint8_t b0 = (p0 >> 16) & 0xFF;

        if ((r0 & 0x7) != 0x0 || (g0 & 0x7) != 0x3 || (b0 & 0x7) == 0) {
            break;
        }

        int lsb_compress = b0 & 0x7;
        int lsb_mask_val = (1 << lsb_compress) - 1;
        long long fifo = 0;
        int fifo_count = 0;
        std::vector<uint8_t> result_bytes;
        std::string length_builder;
        int header_state = STATE_READING_LENGTH;
        size_t final_data_size = 0;
        bool done = false;

        for (int i = 1; i < total_pixels; ++i) {
            uint32_t p = pixels[i];
            uint8_t channels[3];
            channels[0] = p & 0xFF;         // R
            channels[1] = (p >> 8) & 0xFF;  // G
            channels[2] = (p >> 16) & 0xFF; // B

            for (int c = 0; c < 3; ++c) {
                int val = channels[c] & lsb_mask_val;
                fifo = (fifo << lsb_compress) | val;
                fifo_count += lsb_compress;

                if (fifo_count >= 8) {
                    int shift = fifo_count - 8;
                    uint8_t byte = (uint8_t)((fifo >> shift) & 0xFF);
                    fifo_count -= 8;
                    if (header_state != STATE_READING_DATA) {
                        if (header_state == STATE_READING_LENGTH) {
                            if (byte == 0x01) {
                                header_state = STATE_READING_NAME;
                                try {
                                    final_data_size = std::stoi(length_builder);
                                    result_bytes.reserve(final_data_size);
                                } catch (...) {
                                    done = true;
                                }
                            } else {
                                length_builder += (char)byte;
                            }
                        }
                        else if (header_state == STATE_READING_NAME) {
                            if (byte == 0x01) header_state = STATE_READING_MIME;
                        }
                        else if (header_state == STATE_READING_MIME) {
                            if (byte == 0x00) header_state = STATE_READING_DATA;
                        }
                    } else {
                        result_bytes.push_back(byte);
                        if (result_bytes.size() >= final_data_size) {
                            done = true;
                            break;
                        }
                    }
                }
            }
            if (done) break;
        }

        AndroidBitmap_unlockPixels(env, tank_pic);
        tank_pixels_ptr = nullptr;

        if (result_bytes.size() != final_data_size || final_data_size == 0) {
            break;
        }

        lsb_data = env->NewByteArray(final_data_size);
        if (lsb_data == nullptr) break;
        env->SetByteArrayRegion(lsb_data, 0, final_data_size, (jbyte*)result_bytes.data());
        bitmap_factory_class = env->FindClass("android/graphics/BitmapFactory");
        if (bitmap_factory_class == nullptr) break;
        jmethodID decode_byte_array_method = env->GetStaticMethodID(bitmap_factory_class, "decodeByteArray", "([BII)Landroid/graphics/Bitmap;");
        if (decode_byte_array_method == nullptr) break;
        result_bitmap = env->CallStaticObjectMethod(bitmap_factory_class, decode_byte_array_method, lsb_data, 0, (jint)final_data_size);

    } while(false);
    if (tank_pixels_ptr != nullptr) {
        AndroidBitmap_unlockPixels(env, tank_pic);
    }
    env->DeleteLocalRef(lsb_data);
    env->DeleteLocalRef(bitmap_factory_class);

    return result_bitmap;
}
