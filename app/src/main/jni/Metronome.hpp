#pragma once

#include "AudioPlayer.hpp"

#include <vector>
#include <jni.h>

/**
 * A Metronome class designed to play evenly timed beats.
 */
class Metronome {
public:
    AudioPlayer player{};
};
