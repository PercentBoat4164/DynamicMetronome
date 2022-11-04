#pragma once

#include "native.hpp"

#include <oboe/Oboe.h>
#include <vector>
#include <thread>

class AudioPlayer : public oboe::AudioStreamCallback {
public:
    AudioPlayer();

    ~AudioPlayer();

    void start();

    void stop();

    void useSound(std::vector<float> &t_sound);

    void setTempo(double t_tempo);

    void setVolume(float t_volume);

    void _setOnClickCallback(std::function<void()> t_callback);

    void _init();

private:
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames);

    oboe::AudioStream *m_stream{};
    size_t m_soundTracker{};
    std::vector<float> m_sound;
    float m_volume{};
    uint64_t m_frameNumber{};
    uint64_t m_nextClick{};
    uint64_t m_framesPerMinute;
    uint8_t m_channelCount;
    uint64_t m_clickSize;
    std::function<void()> m_callback {[] {}};
    std::atomic<bool> m_kill{false};
    std::thread m_callbackThread{[&] {
        JVMHolder::getInst().vm->GetEnv((void **) &JVMHolder::getInst().callbackEnv, JNI_VERSION_1_6);
        JVMHolder::getInst().vm->AttachCurrentThread(&JVMHolder::getInst().callbackEnv, nullptr);
        while (!m_kill) {{
            std::unique_lock<std::mutex> lock(m_mutex);
            m_condition.wait(lock);
            m_callback();
        }}
        JVMHolder::getInst().vm->DetachCurrentThread();
    }};
    std::mutex m_mutex;
    std::condition_variable m_condition;
};