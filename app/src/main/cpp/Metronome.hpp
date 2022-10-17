#ifndef DYNAMIC_METRONOME_METRONOME_HPP
#define DYNAMIC_METRONOME_METRONOME_HPP

#include "Program.hpp"
#include <oboe/Oboe.h>
#include <thread>

/**
 * A Metronome class designed to play evenly timed beats.
 */
class Metronome : public oboe::AudioStreamCallback {
public:
    Metronome();
    ~Metronome();

    void start();
    void stop();
    void executeProgram();
    void togglePlaying();

    Program *getProgram() {
        return &m_program;
    }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *, void *, int32_t);

    std::vector<double> *m_compiledInstructions{};
    oboe::AudioStream *m_stream{};
    size_t m_soundTracker{};
    std::vector<float> *m_sound;
    double m_volume{};
    int m_tempo{130};
    int m_playHead{};
    Program m_program;
    uint64_t frameNumber{};
    uint64_t nextClick{};
    uint64_t m_samplesPerSecond;
    uint8_t m_channelCount;
};


#endif //DYNAMIC_METRONOME_METRONOME_HPP
