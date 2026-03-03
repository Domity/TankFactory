#include <jni.h>
#include <android/bitmap.h>
#include <arm_neon.h>
#include <pthread.h>
#include <unistd.h>
#include <cstdlib>

inline int min_int(int a, int b) { return a < b ? a : b; }
inline int max_int(int a, int b) { return a > b ? a : b; }

__attribute__((always_inline))
inline uint32_t rgb_to_gray_scalar(uint32_t c) {
    uint32_t r = c & 0xFF;
    uint32_t g = (c >> 8) & 0xFF;
    uint32_t b = (c >> 16) & 0xFF;
    return (r * 19595 + g * 38469 + b * 7472) >> 16;
}

struct ScaleXTable {
    int* src_indices;
    int* weights;
};

ScaleXTable precompute_x_table(int src_w, int dst_w) {
    ScaleXTable table;
    int* mem = new int[dst_w * 2];
    table.src_indices = mem;
    table.weights = mem + dst_w;
    float x_ratio = dst_w > 1 ? (float)(src_w - 1) / (dst_w - 1) : 0;
    for (int x = 0; x < dst_w; ++x) {
        float src_xf = x * x_ratio;
        int src_x = (int)src_xf;
        table.src_indices[x] = src_x;
        table.weights[x] = (int)((src_xf - src_x) * 256.0f);
    }
    return table;
}

__attribute__((always_inline))
inline void get_scaled_gray_row(const uint8_t* srcPixels, int src_stride, int src_w, int src_h,
                                int dst_y, int dst_w, float y_ratio,
                                const int* p_idx, const int* p_w, uint8_t* out_row_buffer) {
    float src_yf = dst_y * y_ratio;
    int src_y = (int)src_yf;
    int y_weight = (int)((src_yf - src_y) * 256.0f);
    int y0 = src_y;
    int y1 = (src_y + 1 < src_h) ? src_y + 1 : src_y;
    const uint32_t* row0 = (const uint32_t*)(srcPixels + y0 * src_stride);
    const uint32_t* row1 = (const uint32_t*)(srcPixels + y1 * src_stride);

    for (int x = 0; x < dst_w; ++x) {
        int sx = p_idx[x];
        int xw = p_w[x];
        int sx_next = (sx + 1 < src_w) ? sx + 1 : sx;
        uint32_t g00 = rgb_to_gray_scalar(row0[sx]);
        uint32_t g01 = rgb_to_gray_scalar(row0[sx_next]);
        uint32_t g10 = rgb_to_gray_scalar(row1[sx]);
        uint32_t g11 = rgb_to_gray_scalar(row1[sx_next]);
        uint32_t g0 = g00 + (((g01 - g00) * xw) >> 8);
        uint32_t g1 = g10 + (((g11 - g10) * xw) >> 8);
        out_row_buffer[x] = (uint8_t)(g0 + (((g1 - g0) * y_weight) >> 8));
    }
}

struct EncodeTaskContext {
    uint32_t start_y;
    uint32_t end_y;

    const uint8_t* pixels1;
    const uint8_t* pixels2;
    uint8_t* outputPixels;

    int info1_stride, info1_w, info1_h;
    int info2_stride, info2_w, info2_h;
    int out_stride, out_w, out_h;

    float y_ratio1, y_ratio2;
    ScaleXTable table1, table2;

    int32_t k1_fixed, k2_fixed;
    int threshold;

    uint8_t* thread_buffer;
};

void* encode_worker_thread(void* arg) {
    auto* ctx = (EncodeTaskContext*)arg;

    int16x8_t v_threshold = vdupq_n_s16((int16_t)ctx->threshold);
    int16x8_t v_255 = vdupq_n_s16(255);
    int16x8_t v_0 = vdupq_n_s16(0);

    uint8_t* row1_gray = ctx->thread_buffer;
    uint8_t* row2_gray = ctx->thread_buffer + ctx->out_w;

    for (uint32_t y = ctx->start_y; y < ctx->end_y; ++y) {
        get_scaled_gray_row(ctx->pixels1, ctx->info1_stride, ctx->info1_w, ctx->info1_h,
                            y, ctx->out_w, ctx->y_ratio1, ctx->table1.src_indices, ctx->table1.weights, row1_gray);
        get_scaled_gray_row(ctx->pixels2, ctx->info2_stride, ctx->info2_w, ctx->info2_h,
                            y, ctx->out_w, ctx->y_ratio2, ctx->table2.src_indices, ctx->table2.weights, row2_gray);

        auto* outRow = (uint32_t*)(ctx->outputPixels + y * ctx->out_stride);
        uint32_t x = 0;

        for (; x <= (uint32_t)ctx->out_w - 8; x += 8) {
            uint8x8_t g1_u8 = vld1_u8(&row1_gray[x]);
            uint8x8_t g2_u8 = vld1_u8(&row2_gray[x]);
            int16x8_t g1_s16 = vreinterpretq_s16_u16(vmovl_u8(g1_u8));
            int16x8_t g2_s16 = vreinterpretq_s16_u16(vmovl_u8(g2_u8));

            int32x4_t v1_lo_32 = vmulq_n_s32(vmovl_s16(vget_low_s16(g1_s16)), ctx->k1_fixed);
            int32x4_t v1_hi_32 = vmulq_n_s32(vmovl_s16(vget_high_s16(g1_s16)), ctx->k1_fixed);
            int16x8_t v1_raw = vcombine_s16(vshrn_n_s32(v1_lo_32, 12), vshrn_n_s32(v1_hi_32, 12));
            int16x8_t v1 = vminq_s16(vmaxq_s16(v1_raw, v_threshold), v_255);

            int32x4_t v2_lo_32 = vmulq_n_s32(vmovl_s16(vget_low_s16(g2_s16)), ctx->k2_fixed);
            int32x4_t v2_hi_32 = vmulq_n_s32(vmovl_s16(vget_high_s16(g2_s16)), ctx->k2_fixed);
            int16x8_t v2_raw = vcombine_s16(vshrn_n_s32(v2_lo_32, 12), vshrn_n_s32(v2_hi_32, 12));
            int16x8_t v2 = vminq_s16(vmaxq_s16(v2_raw, v_0), v_threshold);

            int16x8_t alpha_s16 = vsubq_s16(vaddq_s16(v_255, v2), v1);

            uint8x8x4_t out_vec;
            out_vec.val[0] = vqmovun_s16(v2);         // R
            out_vec.val[1] = out_vec.val[0];          // G
            out_vec.val[2] = out_vec.val[0];          // B
            out_vec.val[3] = vqmovun_s16(alpha_s16);  // A
            vst4_u8((uint8_t*)&outRow[x], out_vec);
        }

        for (; x < (uint32_t)ctx->out_w; ++x) {
            int v1 = min_int(max_int((row1_gray[x] * ctx->k1_fixed) >> 12, ctx->threshold), 255);
            int v2 = min_int(max_int((row2_gray[x] * ctx->k2_fixed) >> 12, 0), ctx->threshold);
            int alpha = 255 - v1 + v2;
            outRow[x] = (alpha << 24) | (v2 << 16) | (v2 << 8) | v2;
        }
    }
    return nullptr;
}


extern "C" JNIEXPORT void JNICALL
Java_com_rbtsoft_tankfactory_miragetank_MirageTankCoder_encodeNative(
        JNIEnv *env, jobject,
        jobject bitmap1, jobject bitmap2, jobject outputBitmap,
        jfloat photo1K, jfloat photo2K, jint threshold) {

    AndroidBitmapInfo info1, info2, outInfo;
    if (AndroidBitmap_getInfo(env, bitmap1, &info1) < 0 ||
        AndroidBitmap_getInfo(env, bitmap2, &info2) < 0 ||
        AndroidBitmap_getInfo(env, outputBitmap, &outInfo) < 0) return;

    void *pixels1, *pixels2, *outputPixels;
    if (AndroidBitmap_lockPixels(env, bitmap1, &pixels1) < 0 ||
        AndroidBitmap_lockPixels(env, bitmap2, &pixels2) < 0 ||
        AndroidBitmap_lockPixels(env, outputBitmap, &outputPixels) < 0) return;

    uint32_t width = outInfo.width;
    uint32_t height = outInfo.height;

    float y_ratio1 = height > 1 ? (float)(info1.height - 1) / (height - 1) : 0;
    float y_ratio2 = height > 1 ? (float)(info2.height - 1) / (height - 1) : 0;

    ScaleXTable table1 = precompute_x_table(info1.width, width);
    ScaleXTable table2 = precompute_x_table(info2.width, width);

    const int32_t FIXED_SHIFT = 12;
    const int32_t k1_fixed = (int32_t)(photo1K * (1 << FIXED_SHIFT));
    const int32_t k2_fixed = (int32_t)(photo2K * (1 << FIXED_SHIFT));

    int num_threads = sysconf(_SC_NPROCESSORS_ONLN);
    if (num_threads <= 0) num_threads = 4;
    if (num_threads > 8) num_threads = 8;
    if (height < (uint32_t)num_threads) num_threads = height;

    pthread_t* threads = new pthread_t[num_threads];
    EncodeTaskContext* tasks = new EncodeTaskContext[num_threads];
    uint8_t* thread_buffers = new uint8_t[num_threads * width * 2];

    uint32_t rows_per_thread = height / num_threads;
    uint32_t remainder = height % num_threads;
    uint32_t current_y = 0;

    for (int i = 0; i < num_threads; ++i) {
        tasks[i].pixels1 = (const uint8_t*)pixels1;
        tasks[i].pixels2 = (const uint8_t*)pixels2;
        tasks[i].outputPixels = (uint8_t*)outputPixels;
        tasks[i].info1_stride = info1.stride; tasks[i].info1_w = info1.width; tasks[i].info1_h = info1.height;
        tasks[i].info2_stride = info2.stride; tasks[i].info2_w = info2.width; tasks[i].info2_h = info2.height;
        tasks[i].out_stride = outInfo.stride; tasks[i].out_w = width;         tasks[i].out_h = height;
        tasks[i].y_ratio1 = y_ratio1; tasks[i].y_ratio2 = y_ratio2;
        tasks[i].table1 = table1;     tasks[i].table2 = table2;
        tasks[i].k1_fixed = k1_fixed; tasks[i].k2_fixed = k2_fixed;
        tasks[i].threshold = threshold;

        tasks[i].thread_buffer = thread_buffers + i * width * 2;

        tasks[i].start_y = current_y;
        uint32_t count = rows_per_thread + (i < (int)remainder ? 1 : 0);
        tasks[i].end_y = current_y + count;
        current_y += count;

        pthread_create(&threads[i], nullptr, encode_worker_thread, &tasks[i]);
    }

    for (int i = 0; i < num_threads; ++i) {
        pthread_join(threads[i], nullptr);
    }

    delete[] threads;
    delete[] tasks;
    delete[] thread_buffers;
    delete[] table1.src_indices;
    delete[] table2.src_indices;

    AndroidBitmap_unlockPixels(env, bitmap1);
    AndroidBitmap_unlockPixels(env, bitmap2);
    AndroidBitmap_unlockPixels(env, outputBitmap);
}