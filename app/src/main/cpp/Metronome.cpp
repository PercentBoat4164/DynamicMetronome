#include "Metronome.hpp"

#include "native.hpp"

#include <jni.h>
#include <thread>

void Metronome::executeProgram() {

}

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
    metronome->player._setOnClickCallback([metronome, globalThiz] {
        jobject obj = JVMHolder::getInst().callbackEnv->NewLocalRef(globalThiz);
        jclass cls = JVMHolder::getInst().callbackEnv->GetObjectClass(obj);
        if (cls) {
            jmethodID method = JVMHolder::getInst().callbackEnv->GetMethodID(cls, "callback","()V");
            if (method)
                metronome->player._setOnClickCallback([method, obj] {
                    JVMHolder::getInst().callbackEnv->CallVoidMethod(obj, method);
                });
        }
    });
    metronome->player._init();
    metronome->player.setVolume(1.0/3);

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
Java_dynamicmetronome_metronome_Metronome_executeProgram(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Metronome *>(handle)->executeProgram();
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