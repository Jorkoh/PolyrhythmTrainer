/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <jni.h>
#include <memory>

#include <android/asset_manager_jni.h>

#include "utils/logging.h"
#include "Game.h"


extern "C" {

std::unique_ptr<Game> game;

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_MainActivity_nativeOnStart(JNIEnv *env,
                                                               jobject instance,
                                                               jobject jAssetManager) {
    AAssetManager *assetManager = AAssetManager_fromJava(env, jAssetManager);
    if (assetManager == nullptr) {
        LOGE("Could not obtain the AAssetManager");
        return;
    }

    game = std::make_unique<Game>(*assetManager);
    game->start();
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_RendererWrapper_nativeOnDrawFrame(JNIEnv *env, jobject type) {
    game->tick();
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_MainActivity_nativeOnStop(JNIEnv *env, jobject instance) {
    game->stop();
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_MainActivity_nativeSetDefaultStreamValues(JNIEnv *env,
                                                                      jobject type,
                                                                      jint sampleRate,
                                                                      jint framesPerBurst) {
    oboe::DefaultStreamValues::SampleRate = (int32_t) sampleRate;
    oboe::DefaultStreamValues::FramesPerBurst = (int32_t) framesPerBurst;
}


JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_ui_TrainerFragment_onPadTouch(JNIEnv *env,
                                                                jobject type,
                                                                jint padPosition,
                                                                jlong timeSinceBoot) {
    game->tap(padPosition, timeSinceBoot);
}

} // extern "C"