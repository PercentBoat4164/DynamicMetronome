#include "AudioPlayer.hpp"

#include <utility>

AudioPlayer::AudioPlayer() : m_sound(std::vector<float>(1, 1)) {
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

    m_framesPerMinute = static_cast<uint64_t>(m_stream->getSampleRate()) * 60;
    m_clickSize = m_framesPerMinute / 130;
    m_channelCount = m_stream->getChannelCount();
}

void AudioPlayer::start() {
    m_soundTracker = 0;
    m_playHead = 0;
    m_nextClick = m_frameNumber;
    oboe::Result result = m_stream->requestStart();
    if (result != oboe::Result::OK) {
        throw std::runtime_error(
                std::string("Error starting m_stream: ") + oboe::convertToText(result));
    }
}

void AudioPlayer::stop() {
    m_stream->requestStop();
}

void AudioPlayer::useSound(std::vector<float> &t_sound) {
    m_sound = std::move(t_sound);
}

void AudioPlayer::setTempo(double t_tempo) {
    m_clickSize = (uint64_t) ((double) m_framesPerMinute / t_tempo);
}

void AudioPlayer::setVolume(float t_volume) {
    m_volume = t_volume;
}

oboe::DataCallbackResult
AudioPlayer::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    uint64_t startFrame = m_frameNumber;
    auto *floatData = (float *) audioData;
    if (instructions.empty()) {
        for (; m_frameNumber < numFrames + startFrame; ++m_frameNumber) {
            if (m_frameNumber >= m_nextClick) {
                m_nextClick += m_clickSize;
                m_soundTracker = 0u;
                m_clickCondition.notify_one();
            }
            auto frame = (m_frameNumber - startFrame) * m_channelCount;
            float sample = m_sound[std::min(m_soundTracker++, m_sound.size() - 1)] * m_volume;
            for (int i = 0; i < m_channelCount; ++i) floatData[frame + i] = sample;
        }
    } else {
        for (; m_frameNumber < numFrames + startFrame; ++m_frameNumber) {
            if (m_frameNumber >= m_nextClick) {
                if (m_playHead >= instructions.size()) {
                    m_stopCondition.notify_one();
                }
                m_nextClick += (uint64_t)((double)m_framesPerMinute / instructions[m_playHead++]);
                m_soundTracker = 0u;
                m_clickCondition.notify_one();
            }
            auto frame = (m_frameNumber - startFrame) * m_channelCount;
            float sample = m_sound[std::min(m_soundTracker++, m_sound.size() - 1)] * m_volume;
            for (int i = 0; i < m_channelCount; ++i) floatData[frame + i] = sample;
        }
    }
    return oboe::DataCallbackResult::Continue;
}

AudioPlayer::~AudioPlayer() {
    m_stream->flush();
    m_stream->close();
    m_onClickCallback = []{};
    m_killOnClickCallbackThread = true;
    m_clickCondition.notify_one();
}

void AudioPlayer::setOnClickCallback(std::function<void()> t_callback) {
    m_onClickCallback = std::move(t_callback);
}

void AudioPlayer::setOnStopCallback(std::function<void()> t_callback) {
    m_onStopCallback = std::move(t_callback);
}

std::function<void()> AudioPlayer::_getOnClickCallback() {
    return m_onClickCallback;
}

void AudioPlayer::_init() {
    m_clickCondition.notify_one();
    m_stopCondition.notify_one();
}

void AudioPlayer::setInstructions(const std::vector<double>& t_instructions) {
    instructions = t_instructions;
}
