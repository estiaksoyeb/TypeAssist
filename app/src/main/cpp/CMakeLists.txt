cmake_minimum_required(VERSION 3.22.1)

project("typeassist")

# Bypassing try_run errors for cross-compilation
set(GGML_MACHINE_SUPPORTS_dotprod_EXITCODE 0 CACHE INTEGER "Bypass try_run" FORCE)
set(GGML_MACHINE_SUPPORTS_i8mm_EXITCODE 0 CACHE INTEGER "Bypass try_run" FORCE)

# Configure llama.cpp build
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "llama.cpp: build examples" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "llama.cpp: build tests" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "llama.cpp: build server" FORCE)

# Enable NEON for ARM64
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(GGML_NATIVE ON CACHE BOOL "llama.cpp: native optimizations" FORCE)
endif()

add_subdirectory(llama.cpp)

add_library(
        typeassist
        SHARED
        native-lib.cpp)

# Include directories - MUST be after add_library
target_include_directories(typeassist PRIVATE llama.cpp/include)

find_library(
        log-lib
        log)

target_link_libraries(
        typeassist
        llama
        ${log-lib})
