#include <jni.h>
#include <android/log.h>
#include <cstring>
#include "turbojpeg.h"

#define LOG_TAG "TurboJpeg"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static const char* get_error_string() {
    return tjGetErrorStr();
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_top_zibin_luban_compression_TurboJpegNative_compress(
    JNIEnv *env,
    jobject thiz,
    jbyteArray rgbData,
    jint width,
    jint height,
    jint quality
) {
    unsigned long jpegSize = 0;
    unsigned char* jpegBuf = nullptr;
    tjhandle handle = nullptr;

    jbyte* inputData = env->GetByteArrayElements(rgbData, nullptr);
    if (!inputData) {
        LOGE("获取输入数据失败 (Failed to get input data)");
        return nullptr;
    }

    handle = tjInitCompress();
    if (!handle) {
        LOGE("初始化压缩器失败 (Failed to initialize compressor): %s", get_error_string());
        env->ReleaseByteArrayElements(rgbData, inputData, JNI_ABORT);
        return nullptr;
    }

    int result = tjCompress2(
        handle,
        (unsigned char*)inputData,
        width,
        0,
        height,
        TJPF_RGB,
        &jpegBuf,
        &jpegSize,
        TJSAMP_444,
        quality,
        TJFLAG_FASTDCT
    );

    env->ReleaseByteArrayElements(rgbData, inputData, JNI_ABORT);

    if (result != 0) {
        LOGE("压缩失败 (Compression failed): %s", get_error_string());
        tjDestroy(handle);
        return nullptr;
    }

    jbyteArray jpegArray = env->NewByteArray((jsize)jpegSize);
    if (!jpegArray) {
        LOGE("创建字节数组失败 (Failed to create byte array)");
        tjFree(jpegBuf);
        tjDestroy(handle);
        return nullptr;
    }

    env->SetByteArrayRegion(jpegArray, 0, (jsize)jpegSize, (jbyte*)jpegBuf);

    tjFree(jpegBuf);
    tjDestroy(handle);

    return jpegArray;
}

extern "C" JNIEXPORT jstring JNICALL
Java_top_zibin_luban_compression_TurboJpegNative_getErrorString(
    JNIEnv *env,
    jobject thiz
) {
    const char* error = get_error_string();
    return env->NewStringUTF(error ? error : "未知错误 (Unknown error)");
}
