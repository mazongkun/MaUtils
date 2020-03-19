//
// Created by mazongkun on 2020/3/18.
//

#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

#include "log_utils.h"
#include "opencv_utils.h"

using namespace cv;

bool save_file(const char *filename, const void *data, size_t size)
{
    FILE *fp = fopen(filename, "wb");
    if (fp == NULL) {
        LOGE("open file %s failed !", filename);
        return false;
    }
    fwrite(data, 1, size, fp);
    fclose(fp);
    LOGI("\"%s\" successfully saved.", filename);
    return true;
}

namespace opencv_utils {
    void RGBA2Nv21(unsigned char *rgba, int width, int height, unsigned char *nv21) {
        if (rgba == nullptr || nv21 == nullptr || width <= 0 || height <= 0)
            return;

        Mat cv_rgba(height, width,     CV_8UC4, rgba);
        Mat cv_i420;

        cvtColor(cv_rgba, cv_i420, COLOR_RGBA2YUV_I420);

        unsigned char *uv = nullptr;
        size_t total = 0;

        uv = (unsigned char *)malloc(static_cast<size_t>(width * height / 2));
        total = static_cast<size_t>(width / 2 * height / 2);
        for (size_t i = 0; i < total; i++){
            uv[2 * i + 1] = cv_i420.data[width * height + i];
            uv[2 * i] = cv_i420.data[width * height + width / 2 * height / 2 + i];
        }
        memcpy(nv21, cv_i420.data, width * height);
        memcpy(nv21 + width * height, uv, width * height / 2);
        free(uv);
    }

    void RGBA2Nv12(unsigned char *rgba, int width, int height, unsigned char *nv12) {
        if (rgba == nullptr || nv12 == nullptr || width <= 0 || height <= 0)
            return;

        Mat cv_rgba(height, width,     CV_8UC4, rgba);
        Mat cv_i420;

        cvtColor(cv_rgba, cv_i420, COLOR_RGBA2YUV_I420);

        unsigned char *uv = nullptr;
        size_t total = 0;

        uv = (unsigned char *)malloc(static_cast<size_t>(width * height / 2));
        total = static_cast<size_t>(width / 2 * height / 2);

        uv = (unsigned char *)malloc(static_cast<size_t>(width * height / 2));
        total = static_cast<size_t>(width / 2 * height / 2);
        for (size_t i = 0; i < total; i++){
            uv[2 * i] = cv_i420.data[width * height + i];
            uv[2 * i + 1] = cv_i420.data[width * height + width / 2 * height / 2 + i];
        }
        memcpy(nv12, cv_i420.data, width * height);
        memcpy(nv12 + width * height, uv, width * height / 2);
        free(uv);
    }
}


