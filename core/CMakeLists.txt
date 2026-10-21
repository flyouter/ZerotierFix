# Builds the ZeroTier JNI wrapper (libZeroTierOneJNI.so) from the official
# ZeroTierOne sources in the `externals/core` submodule.
#
# The upstream java/CMakeLists.txt cannot be used as-is: it still references
# node/C25519.cpp and ext/{lz4,json-parser} which no longer exist, misses the
# prometheus headers and the Android log library. Keep this file in sync with
# upstream when the node/ source layout changes.

cmake_minimum_required(VERSION 3.18)
project(ZeroTierOneJNI)

set(ZT_CORE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../externals/core")

# Glob node sources so newly added/removed files (ECC.cpp, PacketMultiplexer.cpp, ...)
# are picked up automatically.
file(GLOB ZT_CORE_SOURCES "${ZT_CORE_DIR}/node/*.cpp")

add_library(ZeroTierOneJNI SHARED
    ${ZT_CORE_DIR}/osdep/OSUtils.cpp
    ${ZT_CORE_DIR}/java/jni/com_zerotierone_sdk_Node.cpp
    ${ZT_CORE_DIR}/java/jni/ZT_jniutils.cpp
    ${ZT_CORE_DIR}/java/jni/ZT_jnicache.cpp
    ${ZT_CORE_SOURCES}
)

target_include_directories(ZeroTierOneJNI PRIVATE
    ${ZT_CORE_DIR}/include
    ${ZT_CORE_DIR}/node
    ${ZT_CORE_DIR}/ext
    ${ZT_CORE_DIR}/ext/prometheus-cpp-lite-1.0/core/include
    ${ZT_CORE_DIR}/ext/prometheus-cpp-lite-1.0/simpleapi/include
)

target_link_libraries(ZeroTierOneJNI log)
