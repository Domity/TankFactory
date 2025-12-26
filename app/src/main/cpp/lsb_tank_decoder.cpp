#include <jni.h>
#include <android/bitmap.h>
#include <vector>
#include <string>
#include <stdexcept>

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

    if (tank_pixels_ptr != nullptr) {
        AndroidBitmap_unlockPixels(env, tank_pic);
    }

    env->DeleteLocalRef(lsb_data);
    env->DeleteLocalRef(bitmap_factory_class);

    return result_bitmap;
}
