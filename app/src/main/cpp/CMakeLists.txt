cmake_minimum_required(VERSION 3.14)
include(FetchContent)  # Requires CMake 3.14

project("DynamicMetronome")

FetchContent_Declare(oboe GIT_REPOSITORY https://github.com/google/oboe GIT_TAG main)
FetchContent_MakeAvailable(oboe)

add_library(soundPlayer SHARED
        SoundPlayer.cpp
        SoundPlayer.hpp
        )

target_link_libraries(soundPlayer PUBLIC ${oboe})
target_include_directories(soundPlayer PUBLIC ${oboe_SOURCE_DIR}/include)