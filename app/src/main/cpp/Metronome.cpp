#include "Metronome.hpp"

#include "native.hpp"

#include <jni.h>
#include <thread>

void Metronome::executeProgram(std::vector<double> t_instructions) {
    player.stop();
    player.setTempo(t_instructions[0]);
    std::function<void()> oldCallback = player._getOnClickCallback();
    player._setOnClickCallback([this, t_instructions, oldCallback] {
        if (++instruction == t_instructions.size()) {
            player._setOnClickCallback([this, oldCallback] {
                player.stop();
                player._setOnClickCallback(oldCallback);
                m_programEndCondition.notify_one();
                oldCallback();
            });
            instruction = 0;
        }
        player.setTempo(t_instructions[instruction]);
        oldCallback();
    });
    player.start();
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
        jobject obj = JVMHolder::getInst().clickCallbackEnv->NewLocalRef(globalThiz);
        jclass cls = JVMHolder::getInst().clickCallbackEnv->GetObjectClass(obj);
        if (cls) {
            jmethodID method = JVMHolder::getInst().clickCallbackEnv->GetMethodID(cls, "clickCallback", "()V");
            if (method)
                metronome->player._setOnClickCallback([method, obj] {
                    JVMHolder::getInst().clickCallbackEnv->CallVoidMethod(obj, method);
                });
        }
    });
    metronome->player._init();

    // Set up the programEndCallback()
    metronome->m_onProgramEndCallback = [metronome, globalThiz] {
        jobject obj = JVMHolder::getInst().programEndCallbackEnv->NewLocalRef(globalThiz);
        jclass cls = JVMHolder::getInst().programEndCallbackEnv->GetObjectClass(obj);
        if (cls) {
            jmethodID method = JVMHolder::getInst().programEndCallbackEnv->GetMethodID(cls, "programEndCallback", "()V");
            if (method)
                metronome->m_onProgramEndCallback = [method, obj] {
                    JVMHolder::getInst().programEndCallbackEnv->CallVoidMethod(obj, method);
                };
        }
    };
    metronome->m_programEndCondition.notify_one();
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
Java_dynamicmetronome_metronome_Metronome_executeProgram(JNIEnv *env, jobject thiz, jlong handle,
                                                         jdoubleArray instructions) {
    auto *javaSoundData = env->GetDoubleArrayElements(instructions, nullptr);
    std::vector<double> nativeInstructions(env->GetArrayLength(instructions));
    for (int i = 0; i < nativeInstructions.size(); ++i)
        nativeInstructions[i] = (float) javaSoundData[i];
    reinterpret_cast<Metronome *>(handle)->executeProgram(nativeInstructions);
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