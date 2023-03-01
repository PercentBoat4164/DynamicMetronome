cmake_minimum_required(VERSION 3.14)

include(FetchContent)  # Requires CMake 3.14

set(FETCHCONTENT_UPDATES_DISCONNECTED ON)
set(CMAKE_CXX_STANDARD 23)
project("DynamicMetronome")

add_library(metronome SHARED
        native.cpp
        Metronome.cpp
        AudioPlayer.cpp
        )

set(FETCHCONTENT_BASE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/ext)

FetchContent_Declare(oboe GIT_REPOSITORY https://github.com/google/oboe GIT_TAG 1.7.0)
FetchContent_MakeAvailable(oboe)

target_link_libraries(metronome PUBLIC oboe)
