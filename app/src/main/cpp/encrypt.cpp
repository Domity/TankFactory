#include <jni.h>
#include <cstdint>
#include <cstring>
#include <arm_neon.h>

namespace Cipher {
    class ChaCha20Poly1305 {
    private:
        static inline uint32_t rotl(uint32_t v, int n) {
            return (v << n) | (v >> (32 - n));
        }
        static void quarter_round(uint32_t& a, uint32_t& b, uint32_t& c, uint32_t& d) {
            a += b; d ^= a; d = rotl(d, 16);
            c += d; b ^= c; b = rotl(b, 12);
            a += b; d ^= a; d = rotl(d, 8);
            c += d; b ^= c; b = rotl(b, 7);
        }
        static void chacha20_block(uint32_t output[16], const uint32_t input[16]) {
            for (int i = 0; i < 16; ++i) output[i] = input[i];
            for (int i = 0; i < 20; i += 2) {
                quarter_round(output[0], output[4], output[8],  output[12]);
                quarter_round(output[1], output[5], output[9],  output[13]);
                quarter_round(output[2], output[6], output[10], output[14]);
                quarter_round(output[3], output[7], output[11], output[15]);
                quarter_round(output[0], output[5], output[10], output[15]);
                quarter_round(output[1], output[6], output[11], output[12]);
                quarter_round(output[2], output[7], output[8],  output[13]);
                quarter_round(output[3], output[4], output[9],  output[14]);
            }
            for (int i = 0; i < 16; ++i) output[i] += input[i];
        }
        struct Poly1305 {
            uint32_t r[5];
            uint32_t h[5];
            uint32_t pad[4];
            void init(const uint8_t key[32]) {
                h[0] = h[1] = h[2] = h[3] = h[4] = 0;
                uint32_t r0 = key[0] | (key[1] << 8) | (key[2] << 16) | (key[3] << 24);
                uint32_t r1 = key[4] | (key[5] << 8) | (key[6] << 16) | (key[7] << 24);
                uint32_t r2 = key[8] | (key[9] << 8) | (key[10] << 16) | (key[11] << 24);
                uint32_t r3 = key[12] | (key[13] << 8) | (key[14] << 16) | (key[15] << 24);
                r[0] = r0 & 0x03ffffff;
                r[1] = ((r0 >> 26) | (r1 << 6)) & 0x03ffff03;
                r[2] = ((r1 >> 20) | (r2 << 12)) & 0x03e03fff;
                r[3] = ((r2 >> 14) | (r3 << 18)) & 0x001fff03;
                r[4] = (r3 >> 8) & 0x000fe003;
                for (int i = 0; i < 4; ++i) {
                    pad[i] = key[16 + i * 4] | (key[17 + i * 4] << 8) |
                             (key[18 + i * 4] << 16) | (key[19 + i * 4] << 24);
                }
            }
            void blocks(const uint8_t* m, size_t bytes, bool is_final_block_partial = false) {
                uint32_t hibit = is_final_block_partial ? 0 : (1 << 24);
                while (bytes >= 16) {
                    uint32_t m0 = m[0] | (m[1] << 8) | (m[2] << 16) | (m[3] << 24);
                    uint32_t m1 = m[4] | (m[5] << 8) | (m[6] << 16) | (m[7] << 24);
                    uint32_t m2 = m[8] | (m[9] << 8) | (m[10] << 16) | (m[11] << 24);
                    uint32_t m3 = m[12] | (m[13] << 8) | (m[14] << 16) | (m[15] << 24);
                    h[0] += m0 & 0x03ffffff;
                    h[1] += ((m0 >> 26) | (m1 << 6)) & 0x03ffffff;
                    h[2] += ((m1 >> 20) | (m2 << 12)) & 0x03ffffff;
                    h[3] += ((m2 >> 14) | (m3 << 18)) & 0x03ffffff;
                    h[4] += (m3 >> 8) | hibit;
                    uint64_t d0 = (uint64_t)h[0] * r[0] + (uint64_t)h[1] * (r[4] * 5) + (uint64_t)h[2] * (r[3] * 5) + (uint64_t)h[3] * (r[2] * 5) + (uint64_t)h[4] * (r[1] * 5);
                    uint64_t d1 = (uint64_t)h[0] * r[1] + (uint64_t)h[1] * r[0] + (uint64_t)h[2] * (r[4] * 5) + (uint64_t)h[3] * (r[3] * 5) + (uint64_t)h[4] * (r[2] * 5);
                    uint64_t d2 = (uint64_t)h[0] * r[2] + (uint64_t)h[1] * r[1] + (uint64_t)h[2] * r[0] + (uint64_t)h[3] * (r[4] * 5) + (uint64_t)h[4] * (r[3] * 5);
                    uint64_t d3 = (uint64_t)h[0] * r[3] + (uint64_t)h[1] * r[2] + (uint64_t)h[2] * r[1] + (uint64_t)h[3] * r[0] + (uint64_t)h[4] * (r[4] * 5);
                    uint64_t d4 = (uint64_t)h[0] * r[4] + (uint64_t)h[1] * r[3] + (uint64_t)h[2] * r[2] + (uint64_t)h[3] * r[1] + (uint64_t)h[4] * r[0];
                    uint32_t c;
                    c = (uint32_t)(d0 >> 26); h[0] = (uint32_t)d0 & 0x03ffffff; d1 += c;
                    c = (uint32_t)(d1 >> 26); h[1] = (uint32_t)d1 & 0x03ffffff; d2 += c;
                    c = (uint32_t)(d2 >> 26); h[2] = (uint32_t)d2 & 0x03ffffff; d3 += c;
                    c = (uint32_t)(d3 >> 26); h[3] = (uint32_t)d3 & 0x03ffffff; d4 += c;
                    c = (uint32_t)(d4 >> 26); h[4] = (uint32_t)d4 & 0x03ffffff; h[0] += c * 5;
                    c = h[0] >> 26; h[0] &= 0x03ffffff; h[1] += c;
                    m += 16;
                    bytes -= 16;
                }
            }
            void finish(uint8_t mac[16]) {
                uint32_t c = h[1] >> 26; h[1] &= 0x03ffffff; h[2] += c;
                c = h[2] >> 26; h[2] &= 0x03ffffff; h[3] += c;
                c = h[3] >> 26; h[3] &= 0x03ffffff; h[4] += c;
                c = h[4] >> 26; h[4] &= 0x03ffffff; h[0] += c * 5;
                c = h[0] >> 26; h[0] &= 0x03ffffff; h[1] += c;
                uint32_t g0 = h[0] + 5; c = g0 >> 26; g0 &= 0x03ffffff;
                uint32_t g1 = h[1] + c; c = g1 >> 26; g1 &= 0x03ffffff;
                uint32_t g2 = h[2] + c; c = g2 >> 26; g2 &= 0x03ffffff;
                uint32_t g3 = h[3] + c; c = g3 >> 26; g3 &= 0x03ffffff;
                uint32_t g4 = h[4] + c - (1 << 26);
                uint32_t mask = (g4 >> 31) - 1;
                h[0] = (h[0] & ~mask) | (g0 & mask);
                h[1] = (h[1] & ~mask) | (g1 & mask);
                h[2] = (h[2] & ~mask) | (g2 & mask);
                h[3] = (h[3] & ~mask) | (g3 & mask);
                h[4] = (h[4] & ~mask) | (g4 & mask);
                uint64_t f0 = ((uint64_t)h[0] | ((uint64_t)h[1] << 26)) + pad[0] + ((uint64_t)pad[1] << 32);
                uint64_t f1 = (((uint64_t)h[1] >> 6) | ((uint64_t)h[2] << 20) | ((uint64_t)h[3] << 46)) + pad[2] + ((uint64_t)pad[3] << 32);
                mac[0] = (uint8_t)f0; mac[1] = (uint8_t)(f0 >> 8); mac[2] = (uint8_t)(f0 >> 16); mac[3] = (uint8_t)(f0 >> 24);
                mac[4] = (uint8_t)(f0 >> 32); mac[5] = (uint8_t)(f0 >> 40); mac[6] = (uint8_t)(f0 >> 48); mac[7] = (uint8_t)(f0 >> 56);
                mac[8] = (uint8_t)f1; mac[9] = (uint8_t)(f1 >> 8); mac[10] = (uint8_t)(f1 >> 16); mac[11] = (uint8_t)(f1 >> 24);
                mac[12] = (uint8_t)(f1 >> 32); mac[13] = (uint8_t)(f1 >> 40); mac[14] = (uint8_t)(f1 >> 48); mac[15] = (uint8_t)(f1 >> 56);
            }
        };
    public:
        static bool process(uint8_t* data, size_t len, const uint8_t key[32], const uint8_t nonce[12], bool encrypt, uint8_t tag[16]) {
            uint32_t init_state[16];
            init_state[0] = 0x61707865; init_state[1] = 0x3320646e;
            init_state[2] = 0x79622d32; init_state[3] = 0x6b206574;
            for (int i = 0; i < 8; ++i) {
                init_state[4 + i] = key[i * 4] | (key[i * 4 + 1] << 8) | (key[i * 4 + 2] << 16) | (key[i * 4 + 3] << 24);
            }
            init_state[12] = 0;
            init_state[13] = nonce[0] | (nonce[1] << 8) | (nonce[2] << 16) | (nonce[3] << 24);
            init_state[14] = nonce[4] | (nonce[5] << 8) | (nonce[6] << 16) | (nonce[7] << 24);
            init_state[15] = nonce[8] | (nonce[9] << 8) | (nonce[10] << 16) | (nonce[11] << 24);
            uint32_t poly_key_block[16];
            chacha20_block(poly_key_block, init_state);
            uint8_t poly_key[32];
            for (int i = 0; i < 8; ++i) {
                poly_key[i * 4] = (uint8_t)poly_key_block[i];
                poly_key[i * 4 + 1] = (uint8_t)(poly_key_block[i] >> 8);
                poly_key[i * 4 + 2] = (uint8_t)(poly_key_block[i] >> 16);
                poly_key[i * 4 + 3] = (uint8_t)(poly_key_block[i] >> 24);
            }
            Poly1305 poly{};
            poly.init(poly_key);
            if (!encrypt) {
                size_t full_blocks = len / 16;
                poly.blocks(data, full_blocks * 16);
                if (len % 16 != 0) {
                    uint8_t last_block[16] = {0};
                    memcpy(last_block, data + full_blocks * 16, len % 16);
                    last_block[len % 16] = 0x01;
                    poly.blocks(last_block, 16, true);
                }
                uint8_t calc_tag[16];
                poly.finish(calc_tag);
                int diff = 0;
                for (int i = 0; i < 16; ++i) diff |= (calc_tag[i] ^ tag[i]);
                if (diff != 0) {
                    memset(poly_key, 0, sizeof(poly_key));
                    return false;
                }
            }
            uint32_t counter = 1;
            size_t offset = 0;
            uint8_t keystream[64];
            while (offset < len) {
                init_state[12] = counter++;
                uint32_t block[16];
                chacha20_block(block, init_state);
                for (int i = 0; i < 16; ++i) {
                    keystream[i * 4] = (uint8_t)block[i];
                    keystream[i * 4 + 1] = (uint8_t)(block[i] >> 8);
                    keystream[i * 4 + 2] = (uint8_t)(block[i] >> 16);
                    keystream[i * 4 + 3] = (uint8_t)(block[i] >> 24);
                }
                size_t chunk = (len - offset < 64) ? (len - offset) : 64;
                for (size_t i = 0; i < chunk; ++i) {
                    data[offset + i] ^= keystream[i];
                }
                offset += chunk;
            }
            if (encrypt) {
                size_t full_blocks = len / 16;
                poly.blocks(data, full_blocks * 16);
                if (len % 16 != 0) {
                    uint8_t last_block[16] = {0};
                    memcpy(last_block, data + full_blocks * 16, len % 16);
                    last_block[len % 16] = 0x01;
                    poly.blocks(last_block, 16, true);
                }
                poly.finish(tag);
            }
            memset(poly_key, 0, sizeof(poly_key));
            memset(keystream, 0, sizeof(keystream));
            return true;
        }
    };
}
extern "C" JNIEXPORT jint JNICALL
Java_io_github_domity_tankfactory_encrypt_TankCipher_nativeChaCha20Poly1305Process(
        JNIEnv* env, jobject,
        jbyteArray data_array,
        jbyteArray key32_array,
        jbyteArray nonce_array,
        jboolean is_encrypt,
        jbyteArray tag_array)
{
    if (!data_array || !key32_array || !nonce_array || !tag_array) return -1;
    if (env->GetArrayLength(key32_array) < 32 ||
        env->GetArrayLength(nonce_array) < 12 ||
        env->GetArrayLength(tag_array) < 16) {
        return -1;
    }
    jsize data_len = env->GetArrayLength(data_array);
    if (data_len == 0) return 0;
    uint8_t key32[32];
    uint8_t nonce[12];
    uint8_t tag[16];
    env->GetByteArrayRegion(key32_array, 0, 32, reinterpret_cast<jbyte*>(key32));
    env->GetByteArrayRegion(nonce_array, 0, 12, reinterpret_cast<jbyte*>(nonce));
    if (!is_encrypt) {
        env->GetByteArrayRegion(tag_array, 0, 16, reinterpret_cast<jbyte*>(tag));
    }
    void* data_ptr = env->GetPrimitiveArrayCritical(data_array, nullptr);
    if (!data_ptr) return -2;
    bool success = Cipher::ChaCha20Poly1305::process(
            static_cast<uint8_t*>(data_ptr),
            data_len, key32, nonce, is_encrypt, tag
    );
    env->ReleasePrimitiveArrayCritical(data_array, data_ptr, 0);
    if (is_encrypt && success) {
        env->SetByteArrayRegion(tag_array, 0, 16, reinterpret_cast<jbyte*>(tag));
    }
    memset(key32, 0, sizeof(key32));
    memset(nonce, 0, sizeof(nonce));
    memset(tag, 0, sizeof(tag));
    return success ? 0 : -3;
}