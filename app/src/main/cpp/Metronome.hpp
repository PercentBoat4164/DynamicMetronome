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
    double m_volume{};
    int m_tempo{};
    bool m_playing{};
    int m_playHead{};
    std::thread m_thread;
    Program m_program;
};


#endif //DYNAMIC_METRONOME_METRONOME_HPP
