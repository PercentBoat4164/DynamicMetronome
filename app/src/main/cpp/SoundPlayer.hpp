#pragma once

#include <oboe/Oboe.h>

class SoundPlayer : public oboe::AudioStreamCallback {
private:
    oboe::AudioStream *stream{};
public:
    SoundPlayer();

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

    ~SoundPlayer();
};