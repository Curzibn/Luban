cmake_minimum_required(VERSION 3.22.1)

project(turbojpeg-wrapper)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(jniLibsDir "${CMAKE_SOURCE_DIR}/../jniLibs")

add_library(turbojpeg SHARED IMPORTED)
set_target_properties(turbojpeg PROPERTIES
    IMPORTED_LOCATION "${jniLibsDir}/${ANDROID_ABI}/libturbojpeg.so"
)

add_library(turbojpeg-wrapper SHARED
    turbojpeg_wrapper.cpp
)

target_include_directories(turbojpeg-wrapper PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

find_library(log-lib log)

target_link_libraries(turbojpeg-wrapper
    turbojpeg
    ${log-lib}
    android
)
