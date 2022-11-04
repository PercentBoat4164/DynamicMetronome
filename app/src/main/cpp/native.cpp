#include "native.hpp"

JNIEXPORT jint JNI_OnLoad(JavaVM* t_vm, void*) {
    JVMHolder::getInst().vm = t_vm;
    return JNI_VERSION_1_6;
}

JVMHolder &JVMHolder::getInst() {
    static JVMHolder jvmHolder{};
    return jvmHolder;
}
