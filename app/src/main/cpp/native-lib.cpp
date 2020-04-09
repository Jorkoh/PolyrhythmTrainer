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
#include "Engine.h"


extern "C" {

std::unique_ptr<Engine> engine;
jobject engineListener;
jmethodID onTapResultMethod;

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_MainActivity_nativeLoad(JNIEnv *env, jobject instance,
                                                          jobject jAssetManager) {
    // Create the engine
    AAssetManager *assetManager = AAssetManager_fromJava(env, jAssetManager);
    if (assetManager == nullptr) {
        LOGE("Could not obtain the AAssetManager");
        return;
    }
    engine = std::make_unique<Engine>(*assetManager);
    // Load the engine
    engine->requestLoad();
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_MainActivity_nativeUnload(JNIEnv *env, jobject instance) {
    engine->unload();
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_MainActivity_nativeSetDefaultStreamValues(JNIEnv *env, jobject type,
                                                                            jint sampleRate,
                                                                            jint framesPerBurst) {
    oboe::DefaultStreamValues::SampleRate = (int32_t) sampleRate;
    oboe::DefaultStreamValues::FramesPerBurst = (int32_t) framesPerBurst;
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_trainer_customviews_PolyrhythmVisualizer_nativeStartRhythm(JNIEnv *env,
                                                                                                          jobject instance) {
    engine->startRhythm();
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_trainer_customviews_PolyrhythmVisualizer_nativeStopRhythm(JNIEnv *env,
                                                                                                         jobject instance) {
    engine->stopRhythm();
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_trainer_TrainerFragment_nativeRegisterVisualizer(JNIEnv *env,
                                                                                                jobject type,
                                                                                                jobject jVisualizer) {
    // Register stuff for callbacks
    engineListener = env->NewGlobalRef(jVisualizer);
    jclass clazz = env->FindClass("com/jorkoh/polyrhythmtrainer/destinations/trainer/customviews/PolyrhythmVisualizer");
    onTapResultMethod = env->GetMethodID(clazz, "onTapResult", "(IDI)V");
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_trainer_TrainerFragment_nativeUnregisterVisualizer(JNIEnv *env,
                                                                                                  jobject type) {
    env->DeleteGlobalRef(engineListener);
    engineListener = nullptr;
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_trainer_TrainerFragment_nativeSetRhythmSettings(JNIEnv *env,
                                                                                               jobject type,
                                                                                               jint newXNumberOfBeats,
                                                                                               jint newYNumberOfBeats,
                                                                                               jint newBPM) {
    engine->setRhythmSettings(newXNumberOfBeats, newYNumberOfBeats, newBPM);
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_sounds_SoundsFragment_nativeSetSoundAssets(JNIEnv *env,
                                                                                          jobject type,
                                                                                          jstring newLeftPadSound,
                                                                                          jstring newRightPadSound) {
    const char *nativeNewLeftPadSound = env->GetStringUTFChars(newLeftPadSound, nullptr);
    const char *nativeNewRightPadSound = env->GetStringUTFChars(newRightPadSound, nullptr);

    engine->setSoundAssets(nativeNewLeftPadSound, nativeNewRightPadSound);

    env->ReleaseStringUTFChars(newLeftPadSound, nativeNewLeftPadSound);
    env->ReleaseStringUTFChars(newRightPadSound, nativeNewRightPadSound);
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_trainer_customviews_PadView_nativeOnPadTouch(JNIEnv *env, jobject type,
                                                                                            jint padPosition,
                                                                                            jlong timeSinceBoot) {
    TapResultWithTimingAndPosition tapResultWithTimingAndPosition = engine->tap(padPosition, timeSinceBoot);
    if (engineListener != nullptr) {
        env->CallVoidMethod(engineListener, onTapResultMethod,
                            static_cast<jint>(tapResultWithTimingAndPosition.tapResult),
                            static_cast<jdouble>(tapResultWithTimingAndPosition.timing),
                            static_cast<jint >(tapResultWithTimingAndPosition.position));
    }
}

} // extern "C"