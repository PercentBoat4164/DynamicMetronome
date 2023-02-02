#pragma once

#include "native.hpp"

#include "oboe/Oboe.h"
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

    void setOnClickCallback(std::function<void()> t_callback);

    std::function<void()> _getOnClickCallback();

    void _init();

    void setInstructions(const std::vector<double>&);

    void setOnStopCallback(std::function<void()> t_callback);

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
    std::function<void()> m_onClickCallback {[] {}};
    std::atomic<bool> m_killOnClickCallbackThread{false};
    std::mutex m_clickMutex;
    std::condition_variable m_clickCondition;
    std::thread m_onClickCallbackThread{[&] {
        JVMHolder::getInst().vm->GetEnv((void **) &JVMHolder::getInst().clickCallbackEnv, JNI_VERSION_1_6);
        JVMHolder::getInst().vm->AttachCurrentThread(&JVMHolder::getInst().clickCallbackEnv, nullptr);
        while (!m_killOnClickCallbackThread) {{
            std::unique_lock<std::mutex> lock(m_clickMutex);
            m_clickCondition.wait(lock);
            m_onClickCallback();
        }}
        JVMHolder::getInst().vm->DetachCurrentThread();
    }};
    std::function<void()> m_onStopCallback;
    std::atomic<bool> m_killOnStopCallbackThread{false};
    std::mutex m_stopMutex;
    std::condition_variable m_stopCondition;
    std::thread m_onStopCallbackThread{[&] {
        JVMHolder::getInst().vm->GetEnv((void **) &JVMHolder::getInst().stopCallbackEnv, JNI_VERSION_1_6);
        JVMHolder::getInst().vm->AttachCurrentThread(&JVMHolder::getInst().stopCallbackEnv, nullptr);
        while (!m_killOnStopCallbackThread) {{
                std::unique_lock<std::mutex> lock(m_stopMutex);
                m_stopCondition.wait(lock);
                m_onStopCallback();
            }}
        JVMHolder::getInst().vm->DetachCurrentThread();
    }};
    std::vector<double> instructions;
    uint64_t m_playHead{};
};