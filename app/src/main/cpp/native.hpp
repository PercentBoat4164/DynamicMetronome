#pragma once

#include <jni.h>

struct JVMHolder {
public:
    JavaVM *vm;
    JNIEnv *clickCallbackEnv;
    JNIEnv *programEndCallbackEnv;

    static JVMHolder &getInst();

private:
    JVMHolder() = default;
};
