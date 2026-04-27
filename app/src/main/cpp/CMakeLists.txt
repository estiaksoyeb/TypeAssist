cmake_minimum_required(VERSION 3.22.1)

project("typeassist")

# Base optimizations
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

# Enable ARM optimizations for ARMv8-A (64-bit)
set(GGML_ARM_FMA ON CACHE BOOL "" FORCE)
set(GGML_ARM_DOTPROD ON CACHE BOOL "" FORCE)
set(GGML_ARM_I8MM ON CACHE BOOL "" FORCE)

set(GGML_SVE OFF CACHE BOOL "" FORCE)
set(GGML_SVE_CNT OFF CACHE BOOL "" FORCE)

set(GGML_CPU_ALL_VARIANTS OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)

# Try to find Vulkan but don't fail if missing
find_library(vulkan-lib vulkan)
if (vulkan-lib)
    message(STATUS "Vulkan library found, checking for shader compiler...")
    # We only enable Vulkan if we can actually build the shaders
    # In some mobile environments, glslc might be missing
    find_program(GLSLC glslc)
    if (GLSLC)
        set(GGML_VULKAN ON CACHE BOOL "" FORCE)
        add_definitions(-DGGML_VULKAN)
        message(STATUS "Vulkan acceleration enabled")
    else()
        set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
        message(WARNING "glslc not found. Vulkan acceleration disabled.")
    endif()
else()
    set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
endif()

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

if (GGML_VULKAN)
    target_include_directories(typeassist PRIVATE llama.cpp/ggml/src/ggml-vulkan)
endif()

# Standard Android build flags with architecture-specific tuning
# We only support arm64-v8a now
target_compile_options(typeassist PRIVATE -DGGML_USE_CPU -march=armv8.2-a -O3)

find_library(log-lib log)
if (GGML_VULKAN)
    target_link_libraries(typeassist llama ${log-lib} ${vulkan-lib})
else()
    target_link_libraries(typeassist llama ${log-lib})
endif()
