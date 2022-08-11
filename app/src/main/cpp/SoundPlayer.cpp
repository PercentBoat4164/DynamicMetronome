#include <jni.h>
#include "SoundPlayer.hpp"

SoundPlayer::SoundPlayer() {
    oboe::AudioStreamBuilder builder;

    builder.setCallback(this);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);

    oboe::Result result = builder.openStream(&stream);

    if (result != oboe::Result::OK) {
        throw std::runtime_error(std::string("Error opening stream: ") + oboe::convertToText(result));
    }

    oboe::ResultWithValue<int32_t> setBufferSizeResult = stream->setBufferSizeInFrames(stream->getFramesPerBurst() * 2);
    if (setBufferSizeResult) {
        printf("New buffer size is %d frames", setBufferSizeResult.value());
    }

    result = stream->requestStart();

    if (result != oboe::Result::OK) {
        throw std::runtime_error(std::string("Error starting stream: ") + oboe::convertToText(result));
    }
}

oboe::DataCallbackResult SoundPlayer::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    printf("%dms to finish with latency of %fms", numFrames / audioStream->getSampleRate(), stream->calculateLatencyMillis().value());
    stream->write(audioData, numFrames, oboe::kDefaultTimeoutNanos);
    return oboe::DataCallbackResult::Continue;
}

SoundPlayer::~SoundPlayer() {
    stream->flush();
    stream->close();
}

extern "C" JNIEXPORT jstring JNICALL Java_dynamicmetronome_activities_MainActivity_attempt(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF("Hello World!");
}