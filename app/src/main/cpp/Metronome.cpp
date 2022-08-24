#include "Metronome.hpp"
#include <jni.h>
#include <fstream>


Metronome::Metronome() {
    oboe::AudioStreamBuilder builder;

    builder.setCallback(this);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);

    oboe::Result result = builder.openStream(&m_stream);

    if (result != oboe::Result::OK) {
        throw std::runtime_error(
                std::string("Error opening m_stream: ") + oboe::convertToText(result));
    }

    oboe::ResultWithValue <int32_t> setBufferSizeResult = m_stream->setBufferSizeInFrames(
            m_stream->getFramesPerBurst() * 2);
    if (setBufferSizeResult) {
        printf("New buffer size is %d frames", setBufferSizeResult.value());
    }

    result = m_stream->requestStart();

    if (result != oboe::Result::OK) {
        throw std::runtime_error(
                std::string("Error starting m_stream: ") + oboe::convertToText(result));
    }
}

Metronome::~Metronome() {
    m_stream->flush();
    m_stream->close();
}

oboe::DataCallbackResult Metronome::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    printf("%dms to finish with latency of %fms", numFrames / audioStream->getSampleRate(), m_stream->calculateLatencyMillis().value());
    m_stream->write(audioData, numFrames, oboe::kDefaultTimeoutNanos);
    return oboe::DataCallbackResult::Continue;
}

void Metronome::start() {
    stop();
    m_playing = true;
    m_playHead = 0;
    m_thread = std::thread([&] {
        while (m_playing) {
            if (m_playHead < m_program.length()) {
                // Play program
                /**@todo Enqueue sound*/
                std::this_thread::sleep_for(std::chrono::duration<double, std::milli>{(*m_compiledInstructions)[m_playHead]});
            } else if (m_playHead == 0) {
                // Play metronome normally
                /**@ todo Enqueue sound*/
                std::this_thread::sleep_for(std::chrono::duration<double, std::milli>{60000 / m_tempo});
            }
        }
        if (m_compiledInstructions != nullptr) free(m_compiledInstructions);
    });
}

void Metronome::stop() {
    m_playing = false;
    if (m_thread.joinable()) m_thread.join();
}

void Metronome::executeProgram() {
    m_compiledInstructions = m_program.compile();
    start();
}

void Metronome::togglePlaying() {
    m_playing ^= true;
    if (m_playing) start(); else stop();
}

extern "C" JNIEXPORT jlong JNICALL Java_dynamicmetronome_metronome_Metronome_createMetronome(JNIEnv *env, jobject thiz) {
    return reinterpret_cast<jlong>(new Metronome);
}

extern "C" JNIEXPORT void JNICALL Java_dynamicmetronome_metronome_Metronome_destroyMetronome(JNIEnv *env, jobject thiz, jlong handle) {
    delete reinterpret_cast<Metronome *>(handle);
}

extern "C" JNIEXPORT void JNICALL Java_dynamicmetronome_metronome_Metronome_start(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Metronome *>(handle)->start();
}

extern "C" JNIEXPORT void JNICALL Java_dynamicmetronome_metronome_Metronome_stop(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Metronome *>(handle)->stop();
}

extern "C" JNIEXPORT void JNICALL Java_dynamicmetronome_metronome_Metronome_executeProgram(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Metronome *>(handle)->executeProgram();
}

extern "C" JNIEXPORT void JNICALL Java_dynamicmetronome_metronome_Metronome_togglePlaying(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Metronome *>(handle)->togglePlaying();
}

extern "C" JNIEXPORT jlong JNICALL Java_dynamicmetronome_metronome_Metronome_getProgram(JNIEnv *env, jobject thiz, jlong handle) {
    return reinterpret_cast<jlong>(reinterpret_cast<Metronome *>(handle)->getProgram());
}
extern "C" JNIEXPORT jlong JNICALL Java_dynamicmetronome_metronome_Metronome_loadProgram(JNIEnv *env, jobject thiz, jlong handle, jstring path) {
    const char *filePath{env->GetStringUTFChars(path, nullptr)};
    std::ifstream file{};
    file.open(filePath);
    env->ReleaseStringUTFChars(path, filePath);
    file.close();
    Program * program{reinterpret_cast<Metronome *>(handle)->getProgram()->deserialize(file)};
    return reinterpret_cast<jlong >(program);
}

extern "C" JNIEXPORT void JNICALL Java_dynamicmetronome_metronome_Metronome_setTempo(JNIEnv *env, jobject thiz, jlong handle, jint tempo) {
    reinterpret_cast<Metronome *>(handle)->m_tempo = tempo;
}
extern "C" JNIEXPORT void JNICALL Java_dynamicmetronome_metronome_Metronome_setVolume(JNIEnv *env, jobject thiz, jlong handle, jdouble volume) {
    reinterpret_cast<Metronome *>(handle)->m_volume = volume;
}