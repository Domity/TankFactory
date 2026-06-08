#include <jni.h>
#include <cstdint>
#include <cstring>
#include <arm_neon.h>

namespace Cipher {
    class ChaCha20 {
    private:
        uint32x4_t state_0, state_1, state_2, state_3;
        #define ROTL_NEON(v, n) vorrq_u32(vshlq_n_u32(v, n), vshrq_n_u32(v, 32 - n))
        inline void generate_keystream(uint8_t* out) {
            uint32x4_t x0 = state_0;
            uint32x4_t x1 = state_1;
            uint32x4_t x2 = state_2;
            uint32x4_t x3 = state_3;
            for (int i = 0; i < 20; i += 2) {
                x0 = vaddq_u32(x0, x1); x3 = veorq_u32(x3, x0); x3 = ROTL_NEON(x3, 16);
                x2 = vaddq_u32(x2, x3); x1 = veorq_u32(x1, x2); x1 = ROTL_NEON(x1, 12);
                x0 = vaddq_u32(x0, x1); x3 = veorq_u32(x3, x0); x3 = ROTL_NEON(x3, 8);
                x2 = vaddq_u32(x2, x3); x1 = veorq_u32(x1, x2); x1 = ROTL_NEON(x1, 7);
                x1 = vextq_u32(x1, x1, 1);
                x2 = vextq_u32(x2, x2, 2);
                x3 = vextq_u32(x3, x3, 3);
                x0 = vaddq_u32(x0, x1); x3 = veorq_u32(x3, x0); x3 = ROTL_NEON(x3, 16);
                x2 = vaddq_u32(x2, x3); x1 = veorq_u32(x1, x2); x1 = ROTL_NEON(x1, 12);
                x0 = vaddq_u32(x0, x1); x3 = veorq_u32(x3, x0); x3 = ROTL_NEON(x3, 8);
                x2 = vaddq_u32(x2, x3); x1 = veorq_u32(x1, x2); x1 = ROTL_NEON(x1, 7);
                x1 = vextq_u32(x1, x1, 3);
                x2 = vextq_u32(x2, x2, 2);
                x3 = vextq_u32(x3, x3, 1);
            }
            x0 = vaddq_u32(x0, state_0);
            x1 = vaddq_u32(x1, state_1);
            x2 = vaddq_u32(x2, state_2);
            x3 = vaddq_u32(x3, state_3);
            vst1q_u32((uint32_t*)(out), x0);
            vst1q_u32((uint32_t*)(out + 16), x1);
            vst1q_u32((uint32_t*)(out + 32), x2);
            vst1q_u32((uint32_t*)(out + 48), x3);
            state_3 = vsetq_lane_u32(vgetq_lane_u32(state_3, 0) + 1, state_3, 0);
        }

    public:
        ChaCha20(const uint8_t key[32], const uint8_t nonce[12]) {
            uint32_t init_0[4] = {0x61707865, 0x3320646e, 0x79622d32, 0x6b206574};
            state_0 = vld1q_u32(init_0);
            state_1 = vld1q_u32((const uint32_t*)key);
            state_2 = vld1q_u32((const uint32_t*)(key + 16));
            uint32_t init_3[4];
            init_3[0] = 0;
            memcpy(&init_3[1], nonce, 12);
            state_3 = vld1q_u32(init_3);
        }
        inline void process(uint8_t* data, size_t len) {
            uint8_t keystream[64];
            size_t i = 0;
            while (i + 64 <= len) {
                generate_keystream(keystream);
                for (int j = 0; j < 64; j += 16) {
                    uint8x16_t in_vec = vld1q_u8(data + i + j);
                    uint8x16_t key_vec = vld1q_u8(keystream + j);
                    vst1q_u8(data + i + j, veorq_u8(in_vec, key_vec));
                }
                i += 64;
            }
            if (i < len) {
                generate_keystream(keystream);
                for (size_t k = 0; i < len; ++i, ++k) {
                    data[i] ^= keystream[k];
                }
            }
        }
    };
}
extern "C" JNIEXPORT void JNICALL
Java_com_rbtsoft_tankfactory_obfuscation_TankCipher_nativeChaCha20Process(JNIEnv* env, jobject,jbyteArray data_array,jbyteArray key32_array)
{
    uint8_t key32[32];
    env->GetByteArrayRegion(key32_array, 0, 32, reinterpret_cast<jbyte*>(key32));
    jsize data_len = env->GetArrayLength(data_array);
    if (data_len <= 12) return;
    void* data_ptr = env->GetPrimitiveArrayCritical(data_array, nullptr);
    if (!data_ptr) return;
    auto* raw_mem = static_cast<uint8_t*>(data_ptr);
    uint8_t nonce[12];
    memcpy(nonce, raw_mem, 12);
    uint8_t* payload_ptr = raw_mem + 12;
    size_t payload_len = data_len - 12;
    Cipher::ChaCha20 engine(key32, nonce);
    engine.process(payload_ptr, payload_len);
    env->ReleasePrimitiveArrayCritical(data_array, data_ptr, 0);
}