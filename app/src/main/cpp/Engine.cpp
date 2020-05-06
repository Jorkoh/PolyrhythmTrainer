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
    currentEngineXMeasure = 1;
    currentEngineYMeasure = 1;
    currentPlayerXMeasure = 1;
    currentPlayerYMeasure = 1;
    nextXBeat = 0;
    nextYBeat = 0;
    currentFrame = 0;
    rhythmPositionMs = 0;
    lastUpdateTime = 0;
}

void Engine::scheduleNewEventsAndWindows() {
    for (int i = 0; i < numberOfXBeats; i++) {
        xBeats[i] = measureLengthMs * i / numberOfXBeats;
    }
    for (int i = 0; i < numberOfYBeats; i++) {
        yBeats[i] = measureLengthMs * i / numberOfYBeats;
    }
}

void Engine::setBpm(int32_t newBpm) {
    measureLengthMs = numberOfYBeats * 60000 / newBpm;
    windowCenterOffsetMs = measureLengthMs * windowCenterOffsetPercentage;
    bpm = newBpm;
}

void Engine::setXNumberOfBeats(int32_t newXNumberOfBeats) {
    numberOfXBeats = newXNumberOfBeats;
}

void Engine::setYNumberOfBeats(int32_t newYNumberOfBeats) {
    measureLengthMs = newYNumberOfBeats * 60000 / bpm;
    windowCenterOffsetMs = measureLengthMs * windowCenterOffsetPercentage;
    numberOfYBeats = newYNumberOfBeats;
}

void Engine::setModeSettings(int32_t newEngineMeasures, int32_t newPlayerMeasures,
                             float newWindowCenterOffsetPercentage) {
    engineMeasures = newEngineMeasures;
    playerMeasures = newPlayerMeasures;
    windowCenterOffsetPercentage = newWindowCenterOffsetPercentage;
    windowCenterOffsetMs = measureLengthMs * windowCenterOffsetPercentage;
}

void Engine::setSoundAssets(const char *newPadSoundFilename, int32_t padPosition,
                            bool withAudioFeedback) {
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

TapResultWithTimingPositionAndMeasure Engine::tap(int32_t padPosition, int64_t eventTimeAsUptime) {
    if (padPosition != 0 && padPosition != 1) {
        LOGW("Invalid pad position, ignoring tap event");
        return {TapResult::Error, 0, padPosition, 0};
    }

    if (engineState == EngineState::Loading || engineState == EngineState::FailedToLoad) {
        LOGW("Engine not started, ignoring tap event");
        return {TapResult::Error, 0, padPosition, 0};
    }

    // Enable the sound for the pad
    if (padPosition == 0) {
        leftPadSound->setPlaying(true);
    } else {
        rightPadSound->setPlaying(true);
    }

    // If we are not measuring the rhythm ignore the tap
    if (engineState != EngineState::MeasuringRhythm) {
        LOGW("Engine not in measuring state, ignoring tap event");
        return {TapResult::Ignored, 0, padPosition, 0};
    }
    // Otherwise try to match it with the window
    if (padPosition == 0) {
        if (currentPlayerYMeasure <= playerMeasures || playerMeasures < 0) {
            // Convert the tap time to a song position
            int64_t tapTimeInRhythmMs = rhythmPositionMs + (eventTimeAsUptime - lastUpdateTime);
            int64_t measureTimeInRhythmMs = tapTimeInRhythmMs - measureLengthMs * (engineMeasures + (currentPlayerYMeasure - 1));
            TapResultWithTimingPositionAndMeasure result = {getTapResult(measureTimeInRhythmMs, yBeats[nextYBeat]),
                                                            measureTimeInRhythmMs / (double) measureLengthMs, padPosition,
                                                            currentPlayerYMeasure};
            nextYBeat = (nextYBeat + 1) % numberOfYBeats;
            if (nextYBeat == 0) {
                currentPlayerYMeasure++;
            }
            return result;
        } else {
            LOGW("No tap window to match, ignoring tap event");
            return {TapResult::Error, 0, padPosition, 0};
        }
    } else {
        if (currentPlayerXMeasure <= playerMeasures || playerMeasures < 0) {
            // Convert the tap time to a song position
            int64_t tapTimeInRhythmMs = rhythmPositionMs + (eventTimeAsUptime - lastUpdateTime);
            int64_t measureTimeInRhythmMs = tapTimeInRhythmMs - measureLengthMs * (engineMeasures + (currentPlayerXMeasure - 1));
            TapResultWithTimingPositionAndMeasure result = {getTapResult(measureTimeInRhythmMs, xBeats[nextXBeat]),
                                                            measureTimeInRhythmMs / (double) measureLengthMs, padPosition,
                                                            currentPlayerXMeasure};
            nextXBeat = (nextXBeat + 1) % numberOfXBeats;
            if (nextXBeat == 0) {
                currentPlayerXMeasure++;
            }
            return result;
        } else {
            LOGW("No tap window to match, ignoring tap event");
            return {TapResult::Error, 0, padPosition, 0};
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
        measurePositionMs = rhythmPositionMs % measureLengthMs;
        int measure = (rhythmPositionMs / measureLengthMs) + 1;

        // If the engine is actively playing the rhythm play the sounds as the events are reached
        if (engineState == EngineState::PlayingRhythm) {
            // Try to get the next rhythm event, if it's time or already past it play the sound
            if (currentEngineXMeasure == measure && measurePositionMs >= xBeats[nextXBeat]) {
                // Right hand plays the x rhythm line
                rightPadSound->setPlaying(true);
                nextXBeat = (nextXBeat + 1) % numberOfXBeats;
                if (nextXBeat == 0) {
                    currentEngineXMeasure++;
                }
            }
            if (currentEngineYMeasure == measure && measurePositionMs >= yBeats[nextYBeat]) {
                // Left hand plays the y rhythm line
                leftPadSound->setPlaying(true);
                nextYBeat = (nextYBeat + 1) % numberOfYBeats;
                if (nextYBeat == 0) {
                    currentEngineYMeasure++;
                }
            }

            // If it's not metronome mode (engineMeasures < 0) and all the beats
            // have been played it's time to measure the user performance
            if (engineMeasures >= 0 && currentEngineXMeasure > engineMeasures && currentEngineYMeasure > engineMeasures) {
                engineState = EngineState::MeasuringRhythm;
            }
        }

        mMixer.renderAudio(outputBuffer + (oboeStream->getChannelCount() * i), 1);
        currentFrame++;
    }

    if (is16Bit) {
        oboe::convertFloatToPcm16(outputBuffer, static_cast<int16_t *>(audioData), numFrames * oboeStream->getChannelCount());
    }

    lastUpdateTime = nowUptimeMillis();

    return DataCallbackResult::Continue;
}

void Engine::onErrorAfterClose(AudioStream *oboeStream, Result error) {
    unload();
    load();
}

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
                 PRId64
                 ", window center offset: %"
                 PRId32, tapTimeInMillis, tapWindowInMillis, windowCenterOffsetMs.load());
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
                AAssetDataSource::newFromCompressedAsset(mAssetManager, leftPadSoundFilename,
                                                         targetProperties)
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
                AAssetDataSource::newFromCompressedAsset(mAssetManager, rightPadSoundFilename,
                                                         targetProperties)
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
