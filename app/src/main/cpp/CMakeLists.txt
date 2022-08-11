cmake_minimum_required(VERSION 3.14)

include(FetchContent)  # Requires CMake 3.14

project("DynamicMetronome")

add_library(soundPlayer SHARED
            SoundPlayer.cpp
            SoundPlayer.hpp
            )


FetchContent_Declare(oboe GIT_REPOSITORY https://github.com/google/oboe GIT_TAG main)
FetchContent_MakeAvailable(oboe)

find_library(log-lib log)

target_link_libraries(soundPlayer PUBLIC ${log-lib} oboe)

target_include_directories(soundPlayer PUBLIC ${oboe_SOURCE_DIR}/include)
