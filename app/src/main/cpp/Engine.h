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

#ifndef RHYTHMGAME_GAME_H
#define RHYTHMGAME_GAME_H

#include <future>

#include <android/asset_manager.h>
#include <oboe/Oboe.h>

#include "shared/Mixer.h"

#include "audio/Player.h"
#include "audio/AAssetDataSource.h"
#include "utils/LockFreeQueue.h"
#include "utils/UtilityFunctions.h"
#include "GameConstants.h"

using namespace oboe;

enum class EngineState {
    // Between requesting load and load complete
    Loading,
    // Load complete unsuccessfully
    FailedToLoad,
    // Load complete successfully, pad sounds enabled, events and windows not active
    Inactive,
    // Playing polyrhythm, pad sounds (?)enabled(?), events active and windows inactive
    PlayingRhythm,
    // Correcting user input of polyrhythm, pad sounds enabled, events inactive and windows active
    MeasuringRhythm
};

class Engine : public AudioStreamCallback {
public:
    explicit Engine(AAssetManager &);

    void load();

    void startRhythm();

    void stopRhythm();

    void resetRhythmPositionEventsAndWindows();

    void scheduleNewEventsAndWindows();

    void unload();

    void setRhythmSettings(int32_t newXNumberOfBeats, int32_t newYNumberOfBeats, int32_t newBPM);

    void setSoundAssets(const char* newPadSoundFilename,int32_t padPosition, bool withAudioFeedback);

    TapResultWithTimingAndPosition tap(int32_t padPosition, int64_t eventTimeAsUptime);

    // Inherited from oboe::AudioStreamCallback
    DataCallbackResult onAudioReady(AudioStream *oboeStream, void *audioData, int32_t numFrames) override;

    void onErrorAfterClose(AudioStream *oboeStream, Result error) override;

private:
    AAssetManager &mAssetManager;
    AudioStream *mAudioStream{nullptr};
    std::future<void> mLoadingResult;
    std::unique_ptr<Player> leftPadSound;
    std::unique_ptr<Player> rightPadSound;
    Mixer mMixer;
    std::unique_ptr<float[]> mConversionBuffer{nullptr}; // For float->int16 conversion
    std::atomic<EngineState> engineState{EngineState::Loading};

    // Left pad sound asset (in assets folder)
    const char* leftPadSoundFilename{"tom1.wav"};
    // Right pad sound asset (in assets folder)
    const char* rightPadSoundFilename{"shaker1.wav"};

    std::atomic<int64_t> rhythmLengthMs{3000};
    std::atomic<int32_t> windowCenterOffsetMs{120};
    std::atomic<int32_t> xNumberOfBeats{3};
    std::atomic<int32_t> yNumberOfBeats{4};

    // The rhythm playing events for the example phase (PlayingRhythm)
    LockFreeQueue<int64_t, kMaxQueueItems> xRhythmEvents;
    LockFreeQueue<int64_t, kMaxQueueItems> yRhythmEvents;

    // The rhythm windows for the user playing phase (MeasuringRhythm)
    LockFreeQueue<int64_t, kMaxQueueItems> xRhythmWindows;
    LockFreeQueue<int64_t, kMaxQueueItems> yRhythmWindows;

    // Progress since the start of the rhythm, used to match events and windows
    std::atomic<int64_t> rhythmPositionMs{0};
    // Used to calculate rhythmPositionMs on onAudioReady
    std::atomic<int64_t> currentFrame{0};
    // Used to sync rhythmPositionMS with eventTimeAsUptime of the taps
    std::atomic<int64_t> lastUpdateTime{0};

    TapResult getTapResult(int64_t tapTimeInMillis, int64_t tapWindowInMillis);

    bool openStream();

    bool setupPadAudioSource(int32_t padPosition);
};


#endif //RHYTHMGAME_GAME_H
