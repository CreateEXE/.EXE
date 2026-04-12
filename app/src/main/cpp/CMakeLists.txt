# Minimum CMake version required for Android NDK
cmake_minimum_required(VERSION 3.22.1)

project("exe_llm_engine")

# Add the native library for our Llama JNI bridge
add_library(
    exe_llm_engine
    SHARED
    llama_jni_bridge.cpp
)

# Find Android log library for debugging native code
find_library(
    log-lib
    log
)

# Link the bridge to the Android log library
target_link_libraries(
    exe_llm_engine
    ${log-lib}
)
