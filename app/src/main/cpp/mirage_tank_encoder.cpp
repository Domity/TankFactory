#include <jni.h>
#include <android/bitmap.h>
#include <arm_neon.h>
#include <pthread.h>
#include <unistd.h>
#include <cstdlib>

inline int min_int(int a, int b) { return a < b ? a : b; }
inline int max_int(int a, int b) { return a > b ? a : b; }

__attribute__((always_inline))
inline uint32_t rgb_to_gray_scalar(uint32_t c, int32_t wr, int32_t wg, int32_t wb) {
    uint32_t r = c & 0xFF;
    uint32_t g = (c >> 8) & 0xFF;
    uint32_t b = (c >> 16) & 0xFF;
    return (r * wr + g * wg + b * wb) >> 16;
}

inline void* alloc_16k_aligned(size_t size) {
    void* ptr = nullptr;
    size_t aligned_size = (size + 16383) & ~16383;
    posix_memalign(&ptr, 16384, aligned_size);
    return ptr;
}

struct ScaleXTable {
    int* src_x;
    int* src_x_next;
    int* weights;
};

ScaleXTable precompute_x_table(int src_w, int dst_w) {
    ScaleXTable table{};
    int* mem = (int*)alloc_16k_aligned(dst_w * 3 * sizeof(int));
    table.src_x = mem;
    table.src_x_next = mem + dst_w;
    table.weights = mem + 2 * dst_w;
    float x_ratio = dst_w > 0 ? (float)src_w / dst_w : 1.0f;

    for (int x = 0; x < dst_w; ++x) {
        float src_xf = (x + 0.5f) * x_ratio - 0.5f;
        if (src_xf < 0) src_xf = 0;
        if (src_xf > src_w - 1) src_xf = src_w - 1;
        int sx = (int)src_xf;
        table.src_x[x] = sx;
        table.src_x_next[x] = (sx + 1 < src_w) ? sx + 1 : sx;
        table.weights[x] = (int)((src_xf - sx) * 256.0f);
    }
    return table;
}

__attribute__((always_inline))
inline void get_scaled_gray_row(const uint8_t* srcPixels, int src_stride, int src_w, int src_h,
                                int dst_y, int dst_w, float y_ratio,
                                const ScaleXTable& table, uint8_t* out_row_buffer, int32_t wr, int32_t wg, int32_t wb) {
    float src_yf = (dst_y + 0.5f) * y_ratio - 0.5f;
    if (src_yf < 0) src_yf = 0;
    if (src_yf > src_h - 1) src_yf = src_h - 1;
    int src_y = (int)src_yf;
    int y_weight = (int)((src_yf - src_y) * 256.0f);
    int y0 = src_y;
    int y1 = (src_y + 1 < src_h) ? src_y + 1 : src_y;
    const uint32_t* row0 = (const uint32_t*)(srcPixels + y0 * src_stride);
    const uint32_t* row1 = (const uint32_t*)(srcPixels + y1 * src_stride);

    for (int x = 0; x < dst_w; ++x) {
        int sx = table.src_x[x];
        int sx_next = table.src_x_next[x];
        int xw = table.weights[x];

        uint32_t g00 = rgb_to_gray_scalar(row0[sx], wr, wg, wb);
        uint32_t g01 = rgb_to_gray_scalar(row0[sx_next], wr, wg, wb);
        uint32_t g10 = rgb_to_gray_scalar(row1[sx], wr, wg, wb);
        uint32_t g11 = rgb_to_gray_scalar(row1[sx_next], wr, wg, wb);
        uint32_t g0 = g00 + (((g01 - g00) * xw) >> 8);
        uint32_t g1 = g10 + (((g11 - g10) * xw) >> 8);
        out_row_buffer[x] = (uint8_t)(g0 + (((g1 - g0) * y_weight) >> 8));
    }
}

__attribute__((always_inline))
inline void get_scaled_color_row(const uint8_t* srcPixels, int src_stride, int src_w, int src_h,
                                 int dst_y, int dst_w, float y_ratio, const ScaleXTable& table,
                                 uint8_t* out_row_r, uint8_t* out_row_g, uint8_t* out_row_b) {
    float src_yf = (dst_y + 0.5f) * y_ratio - 0.5f;
    if (src_yf < 0) src_yf = 0;
    if (src_yf > src_h - 1) src_yf = src_h - 1;
    int src_y = (int)src_yf;
    int y_weight = (int)((src_yf - src_y) * 256.0f);
    int y0 = src_y;
    int y1 = (src_y + 1 < src_h) ? src_y + 1 : src_y;
    const uint32_t* row0 = (const uint32_t*)(srcPixels + y0 * src_stride);
    const uint32_t* row1 = (const uint32_t*)(srcPixels + y1 * src_stride);

    for (int x = 0; x < dst_w; ++x) {
        int sx = table.src_x[x];
        int sx_next = table.src_x_next[x];
        int xw = table.weights[x];

        uint32_t c00 = row0[sx];
        uint32_t c01 = row0[sx_next];
        uint32_t c10 = row1[sx];
        uint32_t c11 = row1[sx_next];

        uint32_t r00 = c00 & 0xFF, g00 = (c00 >> 8) & 0xFF, b00 = (c00 >> 16) & 0xFF;
        uint32_t r01 = c01 & 0xFF, g01 = (c01 >> 8) & 0xFF, b01 = (c01 >> 16) & 0xFF;
        uint32_t r10 = c10 & 0xFF, g10 = (c10 >> 8) & 0xFF, b10 = (c10 >> 16) & 0xFF;
        uint32_t r11 = c11 & 0xFF, g11 = (c11 >> 8) & 0xFF, b11 = (c11 >> 16) & 0xFF;

        uint32_t r0 = r00 + (((r01 - r00) * xw) >> 8);
        uint32_t r1 = r10 + (((r11 - r10) * xw) >> 8);
        out_row_r[x] = (uint8_t)(r0 + (((r1 - r0) * y_weight) >> 8));

        uint32_t g0 = g00 + (((g01 - g00) * xw) >> 8);
        uint32_t g1 = g10 + (((g11 - g10) * xw) >> 8);
        out_row_g[x] = (uint8_t)(g0 + (((g1 - g0) * y_weight) >> 8));

        uint32_t b0 = b00 + (((b01 - b00) * xw) >> 8);
        uint32_t b1 = b10 + (((b11 - b10) * xw) >> 8);
        out_row_b[x] = (uint8_t)(b0 + (((b1 - b0) * y_weight) >> 8));
    }
}

struct GrayEncodeTaskContext {
    uint32_t start_y, end_y;
    const uint8_t *pixels1, *pixels2;
    uint8_t* outputPixels;
    int info1_stride, info1_w, info1_h;
    int info2_stride, info2_w, info2_h;
    [[maybe_unused]] int out_stride, out_w, out_h;
    float y_ratio1, y_ratio2;
    ScaleXTable table1, table2;
    int32_t k1_fixed, k2_fixed;
    int threshold;
    uint8_t* thread_buffer;
    uint32_t row_size;
    int32_t gray_wr, gray_wg, gray_wb;
};

void* gray_encode_worker_thread(void* arg) {
    auto* ctx = (GrayEncodeTaskContext*)arg;
    int16x8_t v_threshold = vdupq_n_s16((int16_t)ctx->threshold);
    int16x8_t v_255 = vdupq_n_s16(255);
    int16x8_t v_0 = vdupq_n_s16(0);

    uint8_t* row1_gray = ctx->thread_buffer;
    uint8_t* row2_gray = ctx->thread_buffer + ctx->row_size;

    for (uint32_t y = ctx->start_y; y < ctx->end_y; ++y) {
        get_scaled_gray_row(ctx->pixels1, ctx->info1_stride, ctx->info1_w, ctx->info1_h,
                            y, ctx->out_w, ctx->y_ratio1, ctx->table1, row1_gray, ctx->gray_wr, ctx->gray_wg, ctx->gray_wb);
        get_scaled_gray_row(ctx->pixels2, ctx->info2_stride, ctx->info2_w, ctx->info2_h,
                            y, ctx->out_w, ctx->y_ratio2, ctx->table2, row2_gray, ctx->gray_wr, ctx->gray_wg, ctx->gray_wb);

        auto* outRow = (uint32_t*)(ctx->outputPixels + y * ctx->out_stride);
        uint32_t x = 0;

        for (; x <= (uint32_t)ctx->out_w - 8; x += 8) {
            uint8x8_t g1_u8 = vld1_u8(&row1_gray[x]);
            uint8x8_t g2_u8 = vld1_u8(&row2_gray[x]);
            int16x8_t g1_s16 = vreinterpretq_s16_u16(vmovl_u8(g1_u8));
            int16x8_t g2_s16 = vreinterpretq_s16_u16(vmovl_u8(g2_u8));

            int32x4_t v1_lo_32 = vmulq_n_s32(vmovl_s16(vget_low_s16(g1_s16)), ctx->k1_fixed);
            int32x4_t v1_hi_32 = vmulq_n_s32(vmovl_s16(vget_high_s16(g1_s16)), ctx->k1_fixed);
            int16x8_t v1 = vminq_s16(vmaxq_s16(vcombine_s16(vshrn_n_s32(v1_lo_32, 12), vshrn_n_s32(v1_hi_32, 12)), v_threshold), v_255);

            int32x4_t v2_lo_32 = vmulq_n_s32(vmovl_s16(vget_low_s16(g2_s16)), ctx->k2_fixed);
            int32x4_t v2_hi_32 = vmulq_n_s32(vmovl_s16(vget_high_s16(g2_s16)), ctx->k2_fixed);
            int16x8_t v2 = vminq_s16(vmaxq_s16(vcombine_s16(vshrn_n_s32(v2_lo_32, 12), vshrn_n_s32(v2_hi_32, 12)), v_0), v_threshold);

            int16x8_t alpha_s16 = vsubq_s16(vaddq_s16(v_255, v2), v1);

            uint8x8x4_t out_vec;
            out_vec.val[0] = vqmovun_s16(v2);
            out_vec.val[1] = out_vec.val[0];
            out_vec.val[2] = out_vec.val[0];
            out_vec.val[3] = vqmovun_s16(alpha_s16);
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

struct ColorEncodeTaskContext {
    uint32_t start_y, end_y;
    const uint8_t *pixels1, *pixels2;
    uint8_t* outputPixels;
    int info1_stride, info1_w, info1_h;
    int info2_stride, info2_w, info2_h;
    [[maybe_unused]] int out_stride, out_w, out_h;
    float y_ratio1, y_ratio2;
    ScaleXTable table1, table2;
    int32_t scale_i_fixed, scale_c_fixed;
    int32_t desat_i_fixed, desat_c_fixed, weight_i_fixed;
    int32_t gray_wr, gray_wg, gray_wb;
    uint8_t* thread_buffer;
    uint32_t row_size;
};

void* color_encode_worker_thread(void* arg) {
    auto* ctx = (ColorEncodeTaskContext*)arg;

    uint8_t* row1_r = ctx->thread_buffer;
    uint8_t* row1_g = ctx->thread_buffer + ctx->row_size;
    uint8_t* row1_b = ctx->thread_buffer + ctx->row_size * 2;
    uint8_t* row2_r = ctx->thread_buffer + ctx->row_size * 3;
    uint8_t* row2_g = ctx->thread_buffer + ctx->row_size * 4;
    uint8_t* row2_b = ctx->thread_buffer + ctx->row_size * 5;

    for (uint32_t y = ctx->start_y; y < ctx->end_y; ++y) {
        get_scaled_color_row(ctx->pixels1, ctx->info1_stride, ctx->info1_w, ctx->info1_h,
                             y, ctx->out_w, ctx->y_ratio1, ctx->table1, row1_r, row1_g, row1_b);
        get_scaled_color_row(ctx->pixels2, ctx->info2_stride, ctx->info2_w, ctx->info2_h,
                             y, ctx->out_w, ctx->y_ratio2, ctx->table2, row2_r, row2_g, row2_b);

        auto* outRow = (uint32_t*)(ctx->outputPixels + y * ctx->out_stride);

        for (uint32_t x = 0; x < (uint32_t)ctx->out_w; ++x) {
            int r1 = row1_r[x];
            int g1 = row1_g[x];
            int b1 = row1_b[x];
            int r2 = row2_r[x];
            int g2 = row2_g[x];
            int b2 = row2_b[x];

            int gray1_original = (r1 * ctx->gray_wr + g1 * ctx->gray_wg + b1 * ctx->gray_wb) >> 16;
            int gray2_original = (r2 * ctx->gray_wr + g2 * ctx->gray_wg + b2 * ctx->gray_wb) >> 16;

            int r1_scaled = (r1 * ctx->scale_i_fixed) >> 12;
            int g1_scaled = (g1 * ctx->scale_i_fixed) >> 12;
            int b1_scaled = (b1 * ctx->scale_i_fixed) >> 12;

            int gray1_scaled = (gray1_original * ctx->scale_i_fixed) >> 12;

            int r1_desat = r1_scaled + ((gray1_scaled - r1_scaled) * ctx->desat_i_fixed >> 12);
            int g1_desat = g1_scaled + ((gray1_scaled - g1_scaled) * ctx->desat_i_fixed >> 12);
            int b1_desat = b1_scaled + ((gray1_scaled - b1_scaled) * ctx->desat_i_fixed >> 12);

            int r2_inv = ((255 << 12) - ((255 - r2) * ctx->scale_c_fixed)) >> 12;
            int g2_inv = ((255 << 12) - ((255 - g2) * ctx->scale_c_fixed)) >> 12;
            int b2_inv = ((255 << 12) - ((255 - b2) * ctx->scale_c_fixed)) >> 12;

            int gray2_scaled = ((255 << 12) - ((255 - gray2_original) * ctx->scale_c_fixed)) >> 12;

            int r2_desat = r2_inv + ((gray2_scaled - r2_inv) * ctx->desat_c_fixed >> 12);
            int g2_desat = g2_inv + ((gray2_scaled - g2_inv) * ctx->desat_c_fixed >> 12);
            int b2_desat = b2_inv + ((gray2_scaled - b2_inv) * ctx->desat_c_fixed >> 12);

            int alpha_raw = 255 + gray1_scaled - gray2_scaled;
            int alpha_i = min_int(max_int(alpha_raw, 0), 255);
            int alpha = max_int(alpha_i, 1);

            int out_r = ((r1_desat - alpha_i + 255 - r2_desat) * ctx->weight_i_fixed >> 12) + alpha_i - 255 + r2_desat;
            int out_g = ((g1_desat - alpha_i + 255 - g2_desat) * ctx->weight_i_fixed >> 12) + alpha_i - 255 + g2_desat;
            int out_b = ((b1_desat - alpha_i + 255 - b2_desat) * ctx->weight_i_fixed >> 12) + alpha_i - 255 + b2_desat;

            out_r = (out_r * 255) / alpha;
            out_g = (out_g * 255) / alpha;
            out_b = (out_b * 255) / alpha;

            out_r = min_int(max_int(out_r, 0), 255);
            out_g = min_int(max_int(out_g, 0), 255);
            out_b = min_int(max_int(out_b, 0), 255);

            outRow[x] = (alpha_i << 24) | (out_b << 16) | (out_g << 8) | out_r;
        }
    }
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_domity_tankfactory_miragetank_MirageTankCoder_encodeGrayNative(
        JNIEnv *env, jobject, jobject bitmap1, jobject bitmap2, jobject outputBitmap,
        jfloat photo1K, jfloat photo2K, jint threshold,
        jfloat grayWeightR, jfloat grayWeightG, jfloat grayWeightB) {

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

    float y_ratio1 = height > 0 ? (float)info1.height / height : 1.0f;
    float y_ratio2 = height > 0 ? (float)info2.height / height : 1.0f;

    ScaleXTable table1 = precompute_x_table(info1.width, width);
    ScaleXTable table2 = precompute_x_table(info2.width, width);

    const auto k1_fixed = (int32_t)(photo1K * 4096.0f);
    const auto k2_fixed = (int32_t)(photo2K * 4096.0f);
    const auto wr = (int32_t)(grayWeightR * 65536.0f);
    const auto wg = (int32_t)(grayWeightG * 65536.0f);
    const auto wb = (int32_t)(grayWeightB * 65536.0f);

    int num_threads = sysconf(_SC_NPROCESSORS_ONLN);
    if (num_threads <= 0) num_threads = 4;
    if (num_threads > 8) num_threads = 8;
    if (height < (uint32_t)num_threads) num_threads = height;

    auto* threads = new pthread_t[num_threads];
    auto* tasks = new GrayEncodeTaskContext[num_threads];

    uint32_t row_size = (width + 63) & ~63;
    auto* thread_buffers = (uint8_t*)alloc_16k_aligned(num_threads * row_size * 2);

    uint32_t rows_per_thread = height / num_threads;
    uint32_t remainder = height % num_threads;
    uint32_t current_y = 0;

    for (int i = 0; i < num_threads; ++i) {
        tasks[i].pixels1 = (const uint8_t*)pixels1; tasks[i].pixels2 = (const uint8_t*)pixels2;
        tasks[i].outputPixels = (uint8_t*)outputPixels;
        tasks[i].info1_stride = info1.stride; tasks[i].info1_w = info1.width; tasks[i].info1_h = info1.height;
        tasks[i].info2_stride = info2.stride; tasks[i].info2_w = info2.width; tasks[i].info2_h = info2.height;
        tasks[i].out_stride = outInfo.stride; tasks[i].out_w = width;         tasks[i].out_h = height;
        tasks[i].y_ratio1 = y_ratio1;         tasks[i].y_ratio2 = y_ratio2;
        tasks[i].table1 = table1;             tasks[i].table2 = table2;
        tasks[i].k1_fixed = k1_fixed;         tasks[i].k2_fixed = k2_fixed;
        tasks[i].threshold = threshold;
        tasks[i].gray_wr = wr;
        tasks[i].gray_wg = wg;
        tasks[i].gray_wb = wb;

        tasks[i].row_size = row_size;
        tasks[i].thread_buffer = thread_buffers + i * row_size * 2;

        tasks[i].start_y = current_y;
        uint32_t count = rows_per_thread + (i < (int)remainder ? 1 : 0);
        tasks[i].end_y = current_y + count;
        current_y += count;

        pthread_create(&threads[i], nullptr, gray_encode_worker_thread, &tasks[i]);
    }

    for (int i = 0; i < num_threads; ++i) pthread_join(threads[i], nullptr);

    delete[] threads;
    delete[] tasks;
    free(thread_buffers);
    free(table1.src_x);
    free(table2.src_x);

    AndroidBitmap_unlockPixels(env, bitmap1);
    AndroidBitmap_unlockPixels(env, bitmap2);
    AndroidBitmap_unlockPixels(env, outputBitmap);
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_domity_tankfactory_miragetank_MirageTankCoder_encodeColorNative(
        JNIEnv *env, jobject, jobject bitmap1, jobject bitmap2, jobject outputBitmap,
        jfloat scaleInner, jfloat scaleCover,
        jfloat desatInner, jfloat desatCover, jfloat weightInner,
        jfloat grayWeightR, jfloat grayWeightG, jfloat grayWeightB) {

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

    float y_ratio1 = height > 0 ? (float)info1.height / height : 1.0f;
    float y_ratio2 = height > 0 ? (float)info2.height / height : 1.0f;

    ScaleXTable table1 = precompute_x_table(info1.width, width);
    ScaleXTable table2 = precompute_x_table(info2.width, width);

    const auto scale_i_fixed = (int32_t)(scaleInner * 4096.0f);
    const auto scale_c_fixed = (int32_t)((1.0f - scaleCover) * 4096.0f);
    const auto desat_i_fixed = (int32_t)(desatInner * 4096.0f);
    const auto desat_c_fixed = (int32_t)(desatCover * 4096.0f);
    const auto weight_i_fixed = (int32_t)(weightInner * 4096.0f);
    const auto wr = (int32_t)(grayWeightR * 65536.0f);
    const auto wg = (int32_t)(grayWeightG * 65536.0f);
    const auto wb = (int32_t)(grayWeightB * 65536.0f);

    int num_threads = sysconf(_SC_NPROCESSORS_ONLN);
    if (num_threads <= 0) num_threads = 4;
    if (num_threads > 8) num_threads = 8;
    if (height < (uint32_t)num_threads) num_threads = height;

    auto* threads = new pthread_t[num_threads];
    auto* tasks = new ColorEncodeTaskContext[num_threads];

    uint32_t row_size = (width + 63) & ~63;
    auto* thread_buffers = (uint8_t*)alloc_16k_aligned(num_threads * row_size * 6);

    uint32_t rows_per_thread = height / num_threads;
    uint32_t remainder = height % num_threads;
    uint32_t current_y = 0;

    for (int i = 0; i < num_threads; ++i) {
        tasks[i].pixels1 = (const uint8_t*)pixels1; tasks[i].pixels2 = (const uint8_t*)pixels2;
        tasks[i].outputPixels = (uint8_t*)outputPixels;
        tasks[i].info1_stride = info1.stride; tasks[i].info1_w = info1.width; tasks[i].info1_h = info1.height;
        tasks[i].info2_stride = info2.stride; tasks[i].info2_w = info2.width; tasks[i].info2_h = info2.height;
        tasks[i].out_stride = outInfo.stride; tasks[i].out_w = width;         tasks[i].out_h = height;
        tasks[i].y_ratio1 = y_ratio1;         tasks[i].y_ratio2 = y_ratio2;
        tasks[i].table1 = table1;             tasks[i].table2 = table2;
        tasks[i].scale_i_fixed = scale_i_fixed;
        tasks[i].scale_c_fixed = scale_c_fixed;
        tasks[i].desat_i_fixed = desat_i_fixed;
        tasks[i].desat_c_fixed = desat_c_fixed;
        tasks[i].weight_i_fixed = weight_i_fixed;
        tasks[i].gray_wr = wr;
        tasks[i].gray_wg = wg;
        tasks[i].gray_wb = wb;

        tasks[i].row_size = row_size;
        tasks[i].thread_buffer = thread_buffers + i * row_size * 6;

        tasks[i].start_y = current_y;
        uint32_t count = rows_per_thread + (i < (int)remainder ? 1 : 0);
        tasks[i].end_y = current_y + count;
        current_y += count;

        pthread_create(&threads[i], nullptr, color_encode_worker_thread, &tasks[i]);
    }

    for (int i = 0; i < num_threads; ++i) pthread_join(threads[i], nullptr);

    delete[] threads;
    delete[] tasks;
    free(thread_buffers);
    free(table1.src_x);
    free(table2.src_x);

    AndroidBitmap_unlockPixels(env, bitmap1);
    AndroidBitmap_unlockPixels(env, bitmap2);
    AndroidBitmap_unlockPixels(env, outputBitmap);
}
