#include <cstdlib>
#include <cstdio>
#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <android/hardware_buffer.h>
#include <android/hardware_buffer_jni.h>
#include <cstring>
#include <lz4.h>

#define ASSERT(cond, ...) if (!(cond)) \
    __android_log_assert(#cond, "DroidCast_raw_log", __VA_ARGS__)
#define CHECKED(ret, cond, ...) if (!(cond)) do { \
    __android_log_print(ANDROID_LOG_ERROR, "DroidCast_raw_log", __VA_ARGS__); \
    return ret; \
} while (0)

extern "C"
JNIEXPORT jint

JNICALL
Java_ink_mol_droidcast_1raw_ScreenCaptorUtils_copyHardwareBufferToByteBufferNative(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   jobject hardware_buffer,
                                                                                   jobject byte_buffer,
                                                                                   jint position,
                                                                                   jint limit) {
    AHardwareBuffer *hardwareBuffer = AHardwareBuffer_fromHardwareBuffer(env, hardware_buffer);
    CHECKED(-1, hardwareBuffer, "invalid hardware buffer");
    AHardwareBuffer_Desc desc = {0};
    AHardwareBuffer_describe(hardwareBuffer, &desc);
    CHECKED(-1, desc.format == AHARDWAREBUFFER_FORMAT_R5G6B5_UNORM,
            "native copy does not support format %" PRIu32, desc.format);
    CHECKED(-1, desc.width == desc.stride,
            "native copy width != stride, width = %" PRIu32 ", stride = %" PRIu32, desc.width,
            desc.stride);
    size_t size = desc.width * desc.height * 2;
    CHECKED(-1, limit - position >= size, "buffer size %d is smaller than image size %zu",
            limit - position, size);
    void *bufferPtr = env->GetDirectBufferAddress(byte_buffer);
    ASSERT(bufferPtr, "failed to get direct byte buffer ptr");
    void *srcPtr;
    int lockResult = AHardwareBuffer_lock(hardwareBuffer, AHARDWAREBUFFER_USAGE_CPU_READ_RARELY, -1,
                                          nullptr, &srcPtr);
    ASSERT(lockResult == 0, "failed to lock hw buffer");
    std::memcpy(bufferPtr, srcPtr, size);
    lockResult = AHardwareBuffer_unlock(hardwareBuffer, nullptr);
    ASSERT(lockResult == 0, "failed to unlock hw buffer");
    return (int) size;
}

extern "C"
JNIEXPORT jint

JNICALL
Java_ink_mol_droidcast_1raw_ScreenCaptorUtils_copyHardwareBufferToByteBufferNativeLz4(JNIEnv *env,
                                                                                      jobject thiz,
                                                                                      jobject hardware_buffer,
                                                                                      jobject byte_buffer,
                                                                                      jint position,
                                                                                      jint limit) {
    AHardwareBuffer *hardwareBuffer = AHardwareBuffer_fromHardwareBuffer(env, hardware_buffer);
    CHECKED(-1, hardwareBuffer, "invalid hardware buffer");
    AHardwareBuffer_Desc desc;
    AHardwareBuffer_describe(hardwareBuffer, &desc);
    CHECKED(-1, desc.format == AHARDWAREBUFFER_FORMAT_R5G6B5_UNORM,
            "native copy does not support format %" PRIu32, desc.format);
    CHECKED(-1, desc.width == desc.stride,
            "native copy width != stride, width = %" PRIu32 ", stride = %" PRIu32, desc.width,
            desc.stride);
    size_t size = desc.width * desc.height * 2;
    void *bufferPtr = env->GetDirectBufferAddress(byte_buffer);
    ASSERT(bufferPtr, "failed to get direct byte buffer ptr");
    void *srcPtr;
    int lockResult = AHardwareBuffer_lock(hardwareBuffer, AHARDWAREBUFFER_USAGE_CPU_READ_RARELY, -1,
                                          nullptr, &srcPtr);
    ASSERT(lockResult == 0, "failed to lock hw buffer");
    int dstSize = LZ4_compress_default((const char *) srcPtr, (char *) bufferPtr, (int) size,
                                       limit - position);
    lockResult = AHardwareBuffer_unlock(hardwareBuffer, nullptr);
    ASSERT(lockResult == 0, "failed to unlock hw buffer");
    return (int) dstSize;
}

extern "C"
JNIEXPORT jint

JNICALL
Java_ink_mol_droidcast_1raw_ScreenCaptorUtils_copyBitmapToByteBufferNativeLz4(JNIEnv *env,
                                                                              jobject thiz,
                                                                              jobject bitmap,
                                                                              jobject byte_buffer,
                                                                              jint position,
                                                                              jint limit) {
    AndroidBitmapInfo info = {0};
    AndroidBitmap_getInfo(env, bitmap, &info);
    CHECKED(-1, info.format == ANDROID_BITMAP_FORMAT_RGB_565, "invalid RGB565 bitmap");
    int size = int(info.stride * info.height);
    void *bufferPtr = env->GetDirectBufferAddress(byte_buffer);
    ASSERT(bufferPtr, "failed to get direct byte buffer ptr");
    void *srcPtr = nullptr;
    AndroidBitmap_lockPixels(env, bitmap, &srcPtr);
    ASSERT(srcPtr, "failed to lock bitmap");
    int dstSize = LZ4_compress_default((const char *) srcPtr, (char *) bufferPtr, (int) size,
                                       limit - position);
    AndroidBitmap_unlockPixels(env, bitmap);
    return (int) dstSize;
}

extern "C"
JNIEXPORT jint

JNICALL
Java_ink_mol_droidcast_1raw_ScreenCaptorUtils_lz4CompressBound(JNIEnv *env, jobject thiz,
                                                               jint data_size) {
    return LZ4_compressBound(data_size);
}
