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
Java_com_jorkoh_polyrhythmtrainer_destinations_TrainerFragment_nativeCreate(JNIEnv *env,
                                                                            jobject instance,
                                                                            jobject jAssetManager) {
    AAssetManager *assetManager = AAssetManager_fromJava(env, jAssetManager);
    if (assetManager == nullptr) {
        LOGE("Could not obtain the AAssetManager");
        return;
    }

    engine = std::make_unique<Engine>(*assetManager);
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_TrainerFragment_nativeLoad(JNIEnv *env, jobject instance) {
    engine->requestLoad();
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_customviews_PolyrhythmVisualizer_nativeStartRhythm(JNIEnv *env,
                                                                                                  jobject instance) {
    // Register stuff for callbacks
    engineListener = env->NewGlobalRef(instance);
    jclass clazz = env->FindClass("com/jorkoh/polyrhythmtrainer/destinations/customviews/PolyrhythmVisualizer");
    onTapResultMethod = env->GetMethodID(clazz, "onTapResult", "(ID)V");
    engine->startRhythm();
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_customviews_PolyrhythmVisualizer_nativeStopRhythm(JNIEnv *env,
                                                                                                 jobject instance) {
    engine->stopRhythm();
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_TrainerFragment_nativeUnload(JNIEnv *env, jobject instance) {
    engine->unload();
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_TrainerFragment_nativeChangeRhythmSettings(JNIEnv *env,
                                                                                          jobject type,
                                                                                          jint newXNumberOfBeats,
                                                                                          jint newYNumberOfBeats,
                                                                                          jint newBPM) {
    engine->changeRhythmSettings(newXNumberOfBeats, newYNumberOfBeats, newBPM);
}

JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_TrainerFragment_nativeSetDefaultStreamValues(JNIEnv *env, jobject type,
                                                                                            jint sampleRate,
                                                                                            jint framesPerBurst) {
    oboe::DefaultStreamValues::SampleRate = (int32_t) sampleRate;
    oboe::DefaultStreamValues::FramesPerBurst = (int32_t) framesPerBurst;
}


JNIEXPORT void JNICALL
Java_com_jorkoh_polyrhythmtrainer_destinations_customviews_PadView_nativeOnPadTouch(JNIEnv *env, jobject type,
                                                                                    jint padPosition,
                                                                                    jlong timeSinceBoot) {
    TapResultWithTiming tapResultWihTiming = engine->tap(padPosition, timeSinceBoot);
    if(engineListener != nullptr){
    env->CallVoidMethod(engineListener, onTapResultMethod,
                        static_cast<jint>(tapResultWihTiming.tapResult),
                        static_cast<jdouble>(tapResultWihTiming.timing));
    }
}

} // extern "C"