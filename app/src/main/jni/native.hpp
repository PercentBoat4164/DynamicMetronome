#pragma once

#include <jni.h>

struct JVMHolder {
public:
    JavaVM *vm;
    JNIEnv *clickCallbackEnv;
    JNIEnv *stopCallbackEnv;

    static JVMHolder &getInst();

private:
    JVMHolder() = default;
};
