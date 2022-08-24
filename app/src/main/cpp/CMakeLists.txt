cmake_minimum_required(VERSION 3.14)

include(FetchContent)  # Requires CMake 3.14

set(FETCHCONTENT_UPDATES_DISCONNECTED ON)
set(CMAKE_CXX_STANDARD 23)

project("DynamicMetronome")

add_library(metronome SHARED
        Metronome.cpp
        Metronome.hpp
        Program.cpp
        Program.hpp
        Instruction.cpp
        Instruction.hpp
        )


FetchContent_Declare(oboe GIT_REPOSITORY https://github.com/google/oboe GIT_TAG main)
FetchContent_MakeAvailable(oboe)

find_library(log-lib log)

target_link_libraries(metronome PUBLIC ${log-lib} oboe)

target_include_directories(metronome PUBLIC ${oboe_SOURCE_DIR}/include)
