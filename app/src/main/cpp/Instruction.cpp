#include "Instruction.hpp"
#include <jni.h>

Instruction::Instruction() = default;

Instruction::Instruction(uint64_t t_tempo, uint64_t t_notesPerMeasure, uint64_t t_typeOfNote, bool t_interpolate, size_t t_barNumber) : m_tempo{t_tempo}, m_notesPerMeasure{t_notesPerMeasure}, m_typeOfNote{t_typeOfNote}, m_interpolate{t_interpolate}, m_barNumber{t_barNumber} {}

void Instruction::setTempo(uint64_t t_tempo) {
    m_tempo = t_tempo;
}

void Instruction::setInterpolation(bool t_interpolate) {
    m_interpolate = t_interpolate;
}

size_t Instruction::getTempo() const {
    return m_tempo;
}

size_t Instruction::getBarNumber() const {
    return m_barNumber;
}

void Instruction::serialize(std::ostream &outputStream) const {
    outputStream << m_tempo << m_interpolate << m_typeOfNote << m_notesPerMeasure << m_barNumber;
}

Instruction *Instruction::deserialize(std::istream &inputStream) {
    inputStream >> m_tempo >> m_interpolate >> m_typeOfNote >> m_notesPerMeasure >> m_barNumber;
    return this;
}

uint64_t Instruction::getNotesPerMeasure() const {
    return m_notesPerMeasure;
}

std::ostream &operator<<(std::ostream &os, const Instruction &instruction) {
    instruction.serialize(os);
    return os;
}

std::istream &operator>>(std::istream &is, Instruction &instruction) {
    instruction.deserialize(is);
    return is;
}

bool Instruction::getInterpolation() const {
    return m_interpolate;
}

extern "C" JNIEXPORT jlong JNICALL Java_dynamicmetronome_metronome_Instruction_createInstruction(JNIEnv *env, jobject thiz) {
    return reinterpret_cast<jlong>(new Instruction);
}

extern "C" JNIEXPORT void JNICALL Java_dynamicmetronome_metronome_Instruction_destroyInstruction(JNIEnv *env, jobject thiz, jlong handle) {
    delete reinterpret_cast<Instruction *>(handle);
}

extern "C" JNIEXPORT void JNICALL Java_dynamicmetronome_metronome_Instruction_setTempo(JNIEnv *env, jobject thiz, jlong handle, jint tempo) {
    reinterpret_cast<Instruction *>(handle)->setTempo((int) tempo);
}

extern "C" JNIEXPORT void JNICALL Java_dynamicmetronome_metronome_Instruction_setInterpolation(JNIEnv *env, jobject thiz, jlong handle, jboolean interpolate) {
    reinterpret_cast<Instruction *>(handle)->setInterpolation((bool) interpolate);
}