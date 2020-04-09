/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <utils/logging.h>
#include <thread>
#include <cinttypes>

#include "Engine.h"

Engine::Engine(AAssetManager &assetManager) : mAssetManager(assetManager) {}

void Engine::requestLoad() {
    engineState = EngineState::Loading;
    // async returns a future, we must store this future to avoid blocking. It's not sufficient
    // to store this in a local variable as its destructor will block until Game::load completes.
    mLoadingResult = std::async(&Engine::load, this);
}

void Engine::load() {
    engineState = EngineState::Loading;
    if (!openStream()) {
        engineState = EngineState::FailedToLoad;
        return;
    }

    if (!setupPadAudioSource(0) || !setupPadAudioSource(1)) {
        engineState = EngineState::FailedToLoad;
        return;
    }

    Result result = mAudioStream->requestStart();
    if (result != Result::OK) {
        LOGE("Failed to start stream. Error: %s", convertToText(result));
        engineState = EngineState::FailedToLoad;
        return;
    }

    engineState = EngineState::Inactive;
}

void Engine::unload() {
    if (mAudioStream != nullptr) {
        mAudioStream->close();
        delete mAudioStream;
        mAudioStream = nullptr;
    }
}

void Engine::startRhythm() {
    resetRhythmPositionEventsAndWindows();
    scheduleNewEventsAndWindows();

    engineState = EngineState::PlayingRhythm;
}


void Engine::stopRhythm() {
    engineState = EngineState::Inactive;
}

void Engine::resetRhythmPositionEventsAndWindows() {
    // TODO clear this queues properly
    int64_t holder;
    while (xRhythmEvents.peek(holder)) {
        xRhythmEvents.pop(holder);
    }
    while (xRhythmWindows.peek(holder)) {
        xRhythmWindows.pop(holder);
    }
    while (yRhythmEvents.peek(holder)) {
        yRhythmEvents.pop(holder);
    }
    while (yRhythmWindows.peek(holder)) {
        yRhythmWindows.pop(holder);
    }
    currentFrame = 0;
    rhythmPositionMs = 0;
    lastUpdateTime = 0;
}

void Engine::scheduleNewEventsAndWindows() {
    for (int index = 0; index < xNumberOfBeats; index++) {
        int64_t xRhythmEvent = rhythmLengthMs * index / xNumberOfBeats;
        xRhythmEvents.push(xRhythmEvent);
        xRhythmWindows.push(xRhythmEvent + rhythmLengthMs);
    }
    for (int index = 0; index < yNumberOfBeats; index++) {
        int64_t yRhythmEvent = rhythmLengthMs * index / yNumberOfBeats;
        yRhythmEvents.push(yRhythmEvent);
        yRhythmWindows.push(yRhythmEvent + rhythmLengthMs);
    }
}

void Engine::setRhythmSettings(int32_t newXNumberOfBeats, int32_t newYNumberOfBeats, int32_t newBPM) {
    rhythmLengthMs = newYNumberOfBeats * 60000 / newBPM;
    // TODO the window relative size will depend on current level
    windowCenterOffsetMs = rhythmLengthMs * 0.04;
    xNumberOfBeats = newXNumberOfBeats;
    yNumberOfBeats = newYNumberOfBeats;
}

void Engine::setSoundAssets(const char *newPadSoundFilename, int32_t padPosition, bool withAudioFeedback) {
    mAudioStream->stop(0);

    if (padPosition == 0) {
        leftPadSoundFilename = newPadSoundFilename;
        setupPadAudioSource(padPosition);
    } else if (padPosition == 1) {
        rightPadSoundFilename = newPadSoundFilename;
        setupPadAudioSource(padPosition);
    }

    mAudioStream->start(0);

    if (withAudioFeedback && padPosition == 0) {
        leftPadSound->setPlaying(true);
    }
    if (withAudioFeedback && padPosition == 1) {
        rightPadSound->setPlaying(true);
    }
}

TapResultWithTimingAndPosition Engine::tap(int32_t padPosition, int64_t eventTimeAsUptime) {
    if (padPosition != 0 && padPosition != 1) {
        LOGW("Invalid pad position, ignoring tap event");
        return {TapResult::Error, 0, padPosition};
    }

    if (engineState == EngineState::Loading || engineState == EngineState::FailedToLoad) {
        LOGW("Engine not started, ignoring tap event");
        return {TapResult::Error, 0, padPosition};
    }

    // Enable the sound for the pad
    if (padPosition == 0) {
        leftPadSound->setPlaying(true);
    } else {
        rightPadSound->setPlaying(true);
    }

    // If we are not measuring the rhythm ignore the tap
    if (engineState != EngineState::MeasuringRhythm) {
        return {TapResult::Ignored, 0, padPosition};
    }
    // Otherwise try to match it with the window
    int64_t nextRhythmWindowTimeMs;
    if (padPosition == 0) {
        if (yRhythmWindows.pop(nextRhythmWindowTimeMs)) {
            // Convert the tap time to a song position
            int64_t tapTimeInRhythmMs = rhythmPositionMs + (eventTimeAsUptime - lastUpdateTime);
            return {getTapResult(tapTimeInRhythmMs, nextRhythmWindowTimeMs),
                    (tapTimeInRhythmMs - rhythmLengthMs) / (double) rhythmLengthMs,
                    padPosition};
        } else {
            LOGW("No tap window to match, ignoring tap event");
            return {TapResult::Error, 0, padPosition};
        }
    } else {
        if (xRhythmWindows.pop(nextRhythmWindowTimeMs)) {
            // Convert the tap time to a song position
            int64_t tapTimeInRhythmMs = rhythmPositionMs + (eventTimeAsUptime - lastUpdateTime);
            return {getTapResult(tapTimeInRhythmMs, nextRhythmWindowTimeMs),
                    (tapTimeInRhythmMs - rhythmLengthMs) / (double) rhythmLengthMs,
                    padPosition};
        } else {
            LOGW("No tap window to match, ignoring tap event");
            return {TapResult::Error, 0, padPosition};
        }
    }
}

DataCallbackResult Engine::onAudioReady(AudioStream *oboeStream, void *audioData, int32_t numFrames) {
    // If our audio stream is expecting 16-bit samples we need to render our floats into a separate
    // buffer then convert them into 16-bit ints
    bool is16Bit = (oboeStream->getFormat() == AudioFormat::I16);
    float *outputBuffer = (is16Bit) ? mConversionBuffer.get() : static_cast<float *>(audioData);

    for (int i = 0; i < numFrames; ++i) {
        rhythmPositionMs = convertFramesToMillis(currentFrame, mAudioStream->getSampleRate());

        // If the engine is actively playing the rhythm play the sounds as the events are reached
        if (engineState == EngineState::PlayingRhythm) {
            // This declaration is compiled out of the loop anyway, it's here for clarity
            int64_t nextClapEventMs;
            // Try to get the next rhythm event, if it's time or already past it play the sound
            if (xRhythmEvents.peek(nextClapEventMs) && rhythmPositionMs >= nextClapEventMs) {
                // Right hand plays the x rhythm line
                rightPadSound->setPlaying(true);
                xRhythmEvents.pop(nextClapEventMs);
            }
            if (yRhythmEvents.peek(nextClapEventMs) && rhythmPositionMs >= nextClapEventMs) {
                // Left hand plays the y rhythm line
                leftPadSound->setPlaying(true);
                yRhythmEvents.pop(nextClapEventMs);
            }

            // All the beats have been played, time to measure the user performance
            if (xRhythmEvents.size() == 0 && yRhythmEvents.size() == 0) {
                engineState = EngineState::MeasuringRhythm;
            }
        }

        mMixer.renderAudio(outputBuffer + (oboeStream->getChannelCount() * i), 1);
        currentFrame++;
    }

    if (is16Bit) {
        oboe::convertFloatToPcm16(
                outputBuffer,
                static_cast<int16_t *>(audioData),
                numFrames * oboeStream->getChannelCount()
        );
    }

    lastUpdateTime = nowUptimeMillis();

    return DataCallbackResult::Continue;
}

void Engine::onErrorAfterClose(AudioStream *oboeStream, Result error) {
    LOGE("The audio stream was closed, please restart the game. Error: %s", convertToText(error));
};

/**
 * Get the result of a tap
 *
 * @param tapTimeInMillis - The time the tap occurred in milliseconds
 * @param tapWindowInMillis - The time at the middle of the "tap window" in milliseconds
 * @return TapResult can be Early, Late or Success
 */
TapResult Engine::getTapResult(int64_t tapTimeInMillis, int64_t tapWindowInMillis) {
    LOGD("Tap time %"
                 PRId64
                 ", tap window time: %"
                 PRId64, tapTimeInMillis, tapWindowInMillis);
    if (tapTimeInMillis <= tapWindowInMillis + windowCenterOffsetMs) {
        if (tapTimeInMillis >= tapWindowInMillis - windowCenterOffsetMs) {
            return TapResult::Success;
        } else {
            return TapResult::Early;
        }
    } else {
        return TapResult::Late;
    }
}

bool Engine::openStream() {
    // Create an audio stream
    AudioStreamBuilder builder;
    builder.setCallback(this);
    builder.setPerformanceMode(PerformanceMode::LowLatency);
    builder.setSharingMode(SharingMode::Exclusive);

    Result result = builder.openStream(&mAudioStream);
    if (result != Result::OK) {
        LOGE("Failed to open stream. Error: %s", convertToText(result));
        return false;
    }

    if (mAudioStream->getFormat() == AudioFormat::I16) {
        mConversionBuffer = std::make_unique<float[]>(
                (size_t) mAudioStream->getBufferCapacityInFrames() *
                mAudioStream->getChannelCount());
    }

    // Reduce stream latency by setting the buffer size to a multiple of the burst size
    auto setBufferSizeResult = mAudioStream->setBufferSizeInFrames(
            mAudioStream->getFramesPerBurst() * kBufferSizeInBursts);
    if (setBufferSizeResult != Result::OK) {
        LOGW("Failed to set buffer size. Error: %s", convertToText(setBufferSizeResult.error()));
    }

    mMixer.setChannelCount(mAudioStream->getChannelCount());

    return true;
}

bool Engine::setupPadAudioSource(int32_t padPosition) {
    // Set the properties of our audio source(s) to match that of our audio stream
    AudioProperties targetProperties{
            .channelCount = mAudioStream->getChannelCount(),
            .sampleRate = mAudioStream->getSampleRate()
    };

    if (padPosition == 0) {
        // Create a data source and player for the left pad sound
        std::shared_ptr<AAssetDataSource> leftPadSoundSource{
                AAssetDataSource::newFromCompressedAsset(mAssetManager, leftPadSoundFilename, targetProperties)
        };
        if (leftPadSoundSource == nullptr) {
            LOGE("Could not load source data for left pad sound");
            return false;
        }
        leftPadSound = std::make_unique<Player>(leftPadSoundSource);
        mMixer.addTrack(leftPadSound.get(), padPosition);
    } else if (padPosition == 1) {
        // Create a data source and player for the left pad sound
        std::shared_ptr<AAssetDataSource> rightPadSoundSource{
                AAssetDataSource::newFromCompressedAsset(mAssetManager, rightPadSoundFilename, targetProperties)
        };
        if (rightPadSoundSource == nullptr) {
            LOGE("Could not load source data for right pad sound");
            return false;
        }
        rightPadSound = std::make_unique<Player>(rightPadSoundSource);
        mMixer.addTrack(rightPadSound.get(), padPosition);
    }

    return true;
}
