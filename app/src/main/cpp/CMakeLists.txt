cmake_minimum_required(VERSION 3.4.1)

set(SRC_DIR       ${CMAKE_SOURCE_DIR})
set(SRC_UTILS_DIR ${CMAKE_SOURCE_DIR}/utils)
set(LIB_DIR       ${CMAKE_SOURCE_DIR}/lib)

include_directories(${SRC_DIR}/include)
include_directories(${SRC_UTILS_DIR})
include_directories(${SRC_UTILS_DIR}/include)

aux_source_directory(${SRC_DIR} SRC_LIST)
aux_source_directory(${SRC_UTILS_DIR} SRC_UTILS_LIST)

# lib
add_library(opencv_lib STATIC IMPORTED)
set_target_properties(opencv_lib PROPERTIES IMPORTED_LOCATION ${LIB_DIR}/${ANDROID_ABI}/libopencv_java4.so)
message(mama= ${LIB_DIR}/${ANDROID_ABI}/libopencv_java4.so)

# jni
add_library(
        native-lib
        SHARED
        ${SRC_LIST}
        ${SRC_UTILS_LIST})

find_library(
        log-lib
        log)

target_link_libraries(
        native-lib
        opencv_lib
        ${log-lib})