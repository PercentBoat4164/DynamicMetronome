
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

include(FetchContent)  # Requires CMake 3.14

# Declares and names the project.

project("DynamicMetronome")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library(soundPlayer STATIC
            SoundPlayer.cpp
            SoundPlayer.hpp
            )


FetchContent_Declare(oboe GIT_REPOSITORY https://github.com/google/oboe GIT_TAG main)
FetchContent_MakeAvailable(oboe)
# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library(log-lib log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries(soundPlayer PUBLIC ${log-lib} ${oboe})

target_include_directories(soundPlayer PUBLIC ${oboe_SOURCE_DIR}/include)
