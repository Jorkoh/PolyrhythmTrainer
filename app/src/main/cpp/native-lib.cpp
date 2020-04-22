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
    engine->load();
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
    if (engine != nullptr) {
        engine->startRhythm();
    }
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_trainer_customviews_PolyrhythmVisualizer_nativeStopRhythm(JNIEnv *env,
                                                                                                         jobject instance) {
    if (engine != nullptr) {
        engine->stopRhythm();
    }
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
Java_com_jorkoh_polyrhythmtrainer_destinations_trainer_TrainerFragment_nativeSetBpm(JNIEnv *env,
                                                                                    jobject type,
                                                                                    jint newBpm) {
    engine->setBpm(newBpm);
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_trainer_TrainerFragment_nativeSetXNumberOfBeats(JNIEnv *env,
                                                                                               jobject type,
                                                                                               jint newXNumberOfBeats) {
    engine->setXNumberOfBeats(newXNumberOfBeats);
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_trainer_TrainerFragment_nativeSetYNumberOfBeats(JNIEnv *env,
                                                                                               jobject type,
                                                                                               jint newYNumberOfBeats) {
    engine->setYNumberOfBeats(newYNumberOfBeats);
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_trainer_TrainerFragment_nativeSetModeSettings(JNIEnv *env,
                                                                                             jobject type,
                                                                                             jint newEngineMeasures,
                                                                                             jint newPlayerMeasures,
                                                                                             jfloat newWindowCenterOffsetPercentage) {
    engine->setModeSettings(newEngineMeasures, newPlayerMeasures, newWindowCenterOffsetPercentage);
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_MainActivity_nativeSetSoundAssets(JNIEnv *env,
                                                                    jobject type,
                                                                    jstring newPadSound,
                                                                    jint padPosition,
                                                                    jboolean withAudioFeedback) {
    const char *holder = env->GetStringUTFChars(newPadSound, nullptr);
    char *nativeNewPadSound = strdup(holder);
    env->ReleaseStringUTFChars(newPadSound, holder);

    engine->setSoundAssets(nativeNewPadSound, (int32_t) padPosition, (bool) withAudioFeedback);
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