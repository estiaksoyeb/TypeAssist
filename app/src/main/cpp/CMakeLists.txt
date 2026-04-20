cmake_minimum_required(VERSION 3.22.1)

project("typeassist")

# Optimized flags for modern Android (ARMv8.2+)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

# Enable critical ARM optimizations
set(GGML_ARM_FMA ON CACHE BOOL "" FORCE)
set(GGML_ARM_DOTPROD ON CACHE BOOL "" FORCE)
set(GGML_ARM_I8MM ON CACHE BOOL "" FORCE)
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

# Use armv8.2-a for better performance while staying compatible
target_compile_options(typeassist PRIVATE -DGGML_USE_CPU -march=armv8.2-a -O3)

find_library(log-lib log)
target_link_libraries(typeassist llama ${log-lib})
