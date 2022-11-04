#pragma once

#include "AudioPlayer.hpp"
#include "Instruction.hpp"

#include <vector>
#include <jni.h>

/**
 * A Metronome class designed to play evenly timed beats.
 */
class Metronome {
public:
    void executeProgram();

    AudioPlayer player{};

private:
    uint64_t m_playHead{};
};
