#include "Metronome.hpp"

#include "native.hpp"

#include <jni.h>
#include <thread>
#include <utility>

extern "C"
JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_useSound(JNIEnv *env, jobject thiz, jlong handle,
                                                   jfloatArray samples) {
    // Convert jfloatArray to std::vector
    auto *javaSoundData = env->GetFloatArrayElements(samples, nullptr);
    std::vector<float> sound(env->GetArrayLength(samples));
    for (int i = 0; i < sound.size(); ++i)
        sound[i] = (float) javaSoundData[i];
    reinterpret_cast<Metronome *>(handle)->player.useSound(sound);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_dynamicmetronome_metronome_Metronome_create(JNIEnv *env, jobject thiz) {
    // Create the metronome and set the callback.
    auto *metronome = new Metronome;

    jobject globalThiz = env->NewGlobalRef(thiz);

    // Extract the callback from the Java class.
    metronome->player.setOnClickCallback([metronome, globalThiz] {
        jobject obj = JVMHolder::getInst().clickCallbackEnv->NewLocalRef(globalThiz);
        jclass cls = JVMHolder::getInst().clickCallbackEnv->GetObjectClass(obj);
        if (cls) {
            jmethodID method = JVMHolder::getInst().clickCallbackEnv->GetMethodID(cls,
                                                                                  "clickCallback",
                                                                                  "()V");
            if (method)
                metronome->player.setOnClickCallback([method, obj] {
                    JVMHolder::getInst().clickCallbackEnv->CallVoidMethod(obj, method);
                });
        }
    })
    ;
    // Set up the m_onStopCallback()
    metronome->player.setOnStopCallback([metronome, globalThiz] {
        jobject obj = JVMHolder::getInst().stopCallbackEnv->NewLocalRef(globalThiz);
        jclass cls = JVMHolder::getInst().stopCallbackEnv->GetObjectClass(obj);
        if (cls) {
            jmethodID method = JVMHolder::getInst().stopCallbackEnv->GetMethodID(cls, "stopCallback", "()V");
            if (method)
                metronome->player.setOnStopCallback([method, obj] {
                    JVMHolder::getInst().stopCallbackEnv->CallVoidMethod(obj, method);
                });
        }
    });
    metronome->player._init();
    // Return a pointer to the metronome for future use.
    return reinterpret_cast<jlong>(metronome);
}

extern "C"
JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_destroy(JNIEnv *env, jobject thiz, jlong handle) {
    delete reinterpret_cast<Metronome *>(handle);
}

extern "C"
JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_start(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Metronome *>(handle)->player.start();
}

extern "C"
JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_stop(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Metronome *>(handle)->player.stop();
}

extern "C"
JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_setProgram(JNIEnv *env, jobject thiz, jlong handle,
                                                     jdoubleArray instructions) {
    auto *javaSoundData = env->GetDoubleArrayElements(instructions, nullptr);
    std::vector<double> nativeInstructions(env->GetArrayLength(instructions));
    for (int i = 0; i < nativeInstructions.size(); ++i)
        nativeInstructions[i] = (double) javaSoundData[i];
    reinterpret_cast<Metronome *>(handle)->player.setInstructions(nativeInstructions);
}

extern "C"
JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_clearProgram(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Metronome *>(handle)->player.setInstructions({});
}

extern "C"
JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_setTempo(JNIEnv *env, jobject thiz, jlong handle,
                                                   jdouble tempo) {
    reinterpret_cast<Metronome *>(handle)->player.setTempo(tempo);
}

extern "C"
JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_setVolume(JNIEnv *env, jobject thiz, jlong handle,
                                                    jfloat volume) {
    reinterpret_cast<Metronome *>(handle)->player.setVolume(volume);
}
extern "C"
JNIEXPORT void JNICALL
Java_dynamicmetronome_metronome_Metronome_resetPlayer(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Metronome *>(handle)->player.reset();
}