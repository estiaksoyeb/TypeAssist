cmake_minimum_required(VERSION 3.22.1)

project("typeassist")

# Base optimizations
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

# Enable ARM optimizations ONLY for ARMv8-A (64-bit)
if (ANDROID_ABI STREQUAL "arm64-v8a")
    set(GGML_ARM_FMA ON CACHE BOOL "" FORCE)
    set(GGML_ARM_DOTPROD ON CACHE BOOL "" FORCE)
    set(GGML_ARM_I8MM ON CACHE BOOL "" FORCE)
else()
    set(GGML_ARM_FMA OFF CACHE BOOL "" FORCE)
    set(GGML_ARM_DOTPROD OFF CACHE BOOL "" FORCE)
    set(GGML_ARM_I8MM OFF CACHE BOOL "" FORCE)
endif()

set(GGML_SVE OFF CACHE BOOL "" FORCE)
set(GGML_SVE_CNT OFF CACHE BOOL "" FORCE)

set(GGML_CPU_ALL_VARIANTS OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)

# Configure llama.cpp build
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)

add_subdirectory(llama.cpp)

# Build a single library
add_library(typeassist SHARED native-lib.cpp)

target_include_directories(typeassist PRIVATE 
    llama.cpp/include
    llama.cpp/ggml/include
    llama.cpp/ggml/src
    llama.cpp/ggml/src/ggml-cpu
)

# Architecture-specific tuning
if (ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(typeassist PRIVATE -DGGML_USE_CPU -march=armv8.2-a -O3)
elseif (ANDROID_ABI STREQUAL "armeabi-v7a")
    target_compile_options(typeassist PRIVATE -DGGML_USE_CPU -mfpu=neon -mfloat-abi=softfp -O3)
else()
    target_compile_options(typeassist PRIVATE -DGGML_USE_CPU -O3)
endif()

# Fix for NDK 27+ armeabi-v7a build (libgcc / undefined version issues)
if (ANDROID_ABI STREQUAL "armeabi-v7a")
    target_link_options(typeassist PRIVATE "-Wl,--undefined-version")
endif()

find_library(log-lib log)
target_link_libraries(typeassist llama ${log-lib})
