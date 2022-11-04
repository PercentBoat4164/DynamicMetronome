#pragma once

#include <jni.h>

struct JVMHolder {
public:
    JavaVM *vm;
    JNIEnv *callbackEnv;

    static JVMHolder &getInst();

private:
    JVMHolder() = default;
};
