#include "Metronome.hpp"
#include <jni.h>
#include <fstream>
#include <chrono>


Metronome::Metronome() : m_sound(new std::vector<float>(1, 1)) {
    oboe::AudioStreamBuilder builder;

    builder.setCallback(this)
            ->setDirection(oboe::Direction::Output)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(oboe::ChannelCount::Stereo)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive);

    oboe::Result result{builder.openStream(&m_stream)};

    if (result != oboe::Result::OK) {
        throw std::runtime_error(
                std::string("Error opening m_stream: ") + oboe::convertToText(result));
    }

    oboe::ResultWithValue<int32_t> setBufferSizeResult{
            m_stream->setBufferSizeInFrames(m_stream->getFramesPerBurst() * 2)};
    if (setBufferSizeResult) {
        printf("New buffer size is %d frames", setBufferSizeResult.value());
    }

    m_samplesPerSecond = m_stream->getSampleRate() * 60;
    m_channelCount = m_stream->getChannelCount();
}

Metronome::~Metronome() {
    m_stream->flush();
    m_stream->close();
}

oboe::DataCallbackResult
Metronome::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    uint64_t startFrame = frameNumber;
    auto *floatData = (float *) audioData;
    for (; frameNumber < numFrames + startFrame; ++frameNumber) {
        if (frameNumber == nextClick) {
            nextClick += m_samplesPerSecond / m_tempo;
            m_soundTracker = 0u;
        }
        for (int i = 0; i < m_channelCount; ++i) {
            floatData[frameNumber - startFrame + i] = (m_soundTracker < m_sound->size() ? (*m_sound)[m_soundTracker++] : 0) * (float) m_volume;
        }
    }
    return oboe::DataCallbackResult::Continue;
}

void Metronome::start() {
    m_playHead = 0;
    nextClick = frameNumber;
    oboe::Result result = m_stream->requestStart();
    if (result != oboe::Result::OK) {
        throw std::runtime_error(
                std::string("Error starting m_stream: ") + oboe::convertToText(result));
    }
}

void Metronome::stop() {
    m_stream->requestStop();
}

void Metronome::executeProgram() {
    m_compiledInstructions = m_program.compile();
    start();
}

void Metronome::togglePlaying() {
    if (m_stream->getState() == oboe::StreamState::Started) stop();
    else start();
}

extern "C" JNIEXPORT jlong JNICALL
Java_dynamicmetronome_metronome_Metronome_create(JNIEnv *env, jobject thiz) {
    return reinterpret_cast<jlong>(new Metronome);
}

extern "C" JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_destroy(JNIEnv *env, jobject thiz,
                                                  jlong handle) {
    delete reinterpret_cast<Metronome *>(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_start(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Metronome *>(handle)->start();
}

extern "C" JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_stop(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Metronome *>(handle)->stop();
}

extern "C" JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_executeProgram(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Metronome *>(handle)->executeProgram();
}

extern "C" JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_togglePlaying(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Metronome *>(handle)->togglePlaying();
}

extern "C" JNIEXPORT jlong JNICALL
Java_dynamicmetronome_metronome_Metronome_getProgram(JNIEnv *env, jobject thiz, jlong handle) {
    return reinterpret_cast<jlong>(reinterpret_cast<Metronome *>(handle)->getProgram());
}
extern "C" JNIEXPORT jlong JNICALL
Java_dynamicmetronome_metronome_Metronome_loadProgram(JNIEnv *env, jobject thiz, jlong handle,
                                                      jstring path) {
    const char *filePath{env->GetStringUTFChars(path, nullptr)};
    std::ifstream file{};
    file.open(filePath);
    env->ReleaseStringUTFChars(path, filePath);
    file.close();
    Program *program{reinterpret_cast<Metronome *>(handle)->getProgram()->deserialize(file)};
    return reinterpret_cast<jlong >(program);
}

extern "C" JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_setTempo(JNIEnv *env, jobject thiz, jlong handle,
                                                   jint tempo) {
    reinterpret_cast<Metronome *>(handle)->m_tempo = tempo;
}
extern "C" JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_setVolume(JNIEnv *env, jobject thiz, jlong handle,
                                                    jdouble volume) {
    reinterpret_cast<Metronome *>(handle)->m_volume = volume;
}
extern "C" JNIEXPORT jdoubleArray JNICALL
Java_dynamicmetronome_metronome_Metronome_getGraphContents(JNIEnv *env, jobject thiz,
                                                           jlong handle) {
    std::map<size_t, Instruction> instructions{
            *reinterpret_cast<Metronome *>(handle)->m_program.getInstructions()};
    std::vector<double> *result{new std::vector<double>};
    result->reserve(instructions.size() * 2);
    size_t previousBar{0};
    for (const auto &[bar, instruction]: instructions) {
        if (!instruction.getInterpolation() && bar != 0) {
            result->push_back(instruction.getTempo());
            result->push_back(previousBar);
        }
        result->push_back(instruction.getTempo());
        result->push_back(bar);
        previousBar = bar;
    }
    jdoubleArray output{env->NewDoubleArray((int) result->size())};
    env->SetDoubleArrayRegion(output, 0, (int) result->size(), result->data());
    return output;
}
extern "C"
JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_useSound(JNIEnv *env, jobject thiz, jlong handle,
                                                   jbyteArray bytes) {
    std::vector<float> *sound = reinterpret_cast<Metronome *>(handle)->m_sound;
    jboolean boolean{false};
    auto thing = env->GetByteArrayElements(bytes, &boolean);
    sound->assign((size_t) env->GetArrayLength(bytes), (float) *thing);
}