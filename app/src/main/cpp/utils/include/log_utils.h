#include <android/log.h>
#include <stdio.h>
#include <string.h>

#ifndef LOG_H_
#define LOG_H_

#define TAG "MA_UTILS"
static bool DEBUG = true;

#ifndef __FILENAME__
#include <string.h>
#define __FILENAME__  (strrchr(__FILE__, '/') + 1)
#endif

#define LOGV(...) if (DEBUG) __android_log_print(ANDROID_LOG_VERBOSE, TAG ,__VA_ARGS__)
#define LOGD(...) if (DEBUG) __android_log_print(ANDROID_LOG_DEBUG, TAG ,__VA_ARGS__)
#define LOGI(...) if (DEBUG) __android_log_print(ANDROID_LOG_INFO, TAG ,__VA_ARGS__)
#define LOGW(...) if (DEBUG) __android_log_print(ANDROID_LOG_WARN, TAG ,__VA_ARGS__)
#define LOGE(...) if (true) __android_log_print(ANDROID_LOG_ERROR, TAG ,__VA_ARGS__)

//#define LOGE2(format, ...) LOGE("[%s][%s][%d]: " format, __FILENAME__, __FUNCTION__, __LINE__, ##__VA_ARGS__)
#define LOGE2(format, ...) LOGE("[%s][%s][%d]: " format "\n", __FILENAME__, __FUNCTION__, __LINE__, ##__VA_ARGS__)
//#define LOGE2(format, ...) printf("[%s][%s][%d]: " format "\n", __FILENAME__, __FUNCTION__, __LINE__, ##__VA_ARGS__)

#endif
