#pragma once

#include "AudioPlayer.hpp"

#include <vector>
#include <jni.h>

/**
 * A Metronome class designed to play evenly timed beats.
 */
class Metronome {
public:
    void executeProgram(std::vector<double> t_instructions);

    AudioPlayer player{};
    size_t instruction{0};

    std::function<void()> m_onProgramEndCallback {[] {}};
    std::atomic<bool> m_killOnProgramEndCallbackThread{false};
    std::thread m_onProgramEndCallbackThread{[&] {
        JVMHolder::getInst().vm->GetEnv((void **) &JVMHolder::getInst().programEndCallbackEnv, JNI_VERSION_1_6);
        JVMHolder::getInst().vm->AttachCurrentThread(&JVMHolder::getInst().programEndCallbackEnv, nullptr);
        while (!m_killOnProgramEndCallbackThread) {{
                std::unique_lock<std::mutex> lock(m_programEndMutex);
                m_programEndCondition.wait(lock);
                instruction = 0;
                player.stop();
                m_onProgramEndCallback();
            }}
        JVMHolder::getInst().vm->DetachCurrentThread();
    }};
    std::mutex m_programEndMutex;
    std::condition_variable m_programEndCondition;

private:
    uint64_t m_playHead{};
};
