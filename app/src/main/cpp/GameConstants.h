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

#ifndef SAMPLES_GAMECONSTANTS_H
#define SAMPLES_GAMECONSTANTS_H


constexpr int kBufferSizeInBursts = 2; // Use 2 bursts as the buffer size (double buffer)
constexpr int kMaxQueueItems = 16; // Must be power of 2

// Filename for left pad sound asset (in assets folder)
constexpr char leftPadSoundFilename[] {"LEFT_PAD_SOUND.mp3" };

// Filename for right sound asset (in assets folder)
constexpr char rightPadSoundFilename[] {"RIGHT_PAD_SOUND.mp3" };

struct AudioProperties {
    int32_t channelCount;
    int32_t sampleRate;
};

#endif //SAMPLES_GAMECONSTANTS_H