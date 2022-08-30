#include "Program.hpp"

#include <jni.h>
#include <sstream>
#include <fstream>

void Program::clear() {
    m_instructions.clear();
    m_highestTempo = 0;
    m_lowestTempo = SIZE_T_MAX;
    m_numBars = 0;
    if (!m_name.empty()) {
        m_name.clear();
    }
}

void Program::addOrChangeInstruction(size_t t_bar, uint64_t t_tempo, bool t_interpolate) {
    m_instructions[std::max(t_bar, size_t(1))] = Instruction{t_tempo, 4, 4, t_interpolate, std::max(t_bar, size_t(1))};
    m_highestTempo = std::max(m_highestTempo, t_tempo);
    m_lowestTempo = std::min(m_lowestTempo, t_tempo);
}

std::vector<double> *Program::compile() {
    // Return nothing if no instructions exist to be compiled.
    if (m_instructions.empty()) return new std::vector<double>{};

    // Convert map of instructions to a vector
    std::vector<Instruction> instructions{};
    instructions.reserve(m_instructions.size());
    // Key : Value = .first : .second
    for (const auto &instruction : m_instructions) instructions.push_back(instruction.second);

    // Vector of compiled instructions to be converted to a jDoubleArray
    auto *compiledInstructions{new std::vector<double>{}};
    compiledInstructions->reserve(m_instructions.size());
    size_t instructionStartingPosition{0};

    // Use the std::generate algorithm to fill the area between each instruction in the array
    for (size_t instruction{0}; instruction <= instructions.size(); ++instruction) {
        double currentTempo{static_cast<double>(instructions[instruction].getTempo())};
        size_t instructionLength{(instructions[instruction].getBarNumber() - instructions[instruction + 1].getBarNumber()) * (size_t) instructions[instruction].getNotesPerMeasure()};
        double tempoChangePerBeat{(currentTempo - instructions[instruction + 1].getTempo()) / instructionLength};
        std::generate(compiledInstructions->begin() + (int) instructionStartingPosition, compiledInstructions->begin() + (int) instructionLength, [&currentTempo, &tempoChangePerBeat] { return currentTempo += tempoChangePerBeat; });
    }

    // This instruction adds a down beat to the end of the program
    compiledInstructions->push_back(0);
    return compiledInstructions;
}

size_t Program::length() const {
    return m_numBars;
}

std::string Program::getName() {
    return m_name;
}

std::map<size_t, Instruction> *Program::getInstructions() {
    return &m_instructions;
}

void Program::serialize(std::ostream &outputStream) const {
    outputStream << m_highestTempo << m_lowestTempo << m_numBars << m_name.size() << m_name << m_instructions.size();
    for (auto &[bar, instruction] : m_instructions) outputStream << bar << instruction;
}

Program *Program::deserialize(std::istream &inputStream) {
    clear();
    inputStream >> m_highestTempo >> m_lowestTempo >> m_numBars;
    std::streamsize dataChunkSize;
    inputStream >> dataChunkSize;
    inputStream.readsome(m_name.data(), dataChunkSize);
    inputStream >> dataChunkSize;
    size_t bar;
    Instruction instruction;
    for (inputStream >> dataChunkSize; dataChunkSize > 0; dataChunkSize -= sizeof(Instruction) + sizeof(size_t)) {
        inputStream >> bar;
        inputStream >> instruction;
        m_instructions[bar] = instruction;
    }
    return this;
}

std::ostream &operator<<(std::ostream &os, const Program &program) {
    program.serialize(os);
    return os;
}

std::istream &operator>>(std::istream &is, Program &program) {
    program.deserialize(is);
    return is;
}

uint64_t Program::getHighestTempo() const {
    return m_highestTempo;
}

uint64_t Program::getLowestTempo() const {
    return m_lowestTempo;
}

extern "C" JNIEXPORT jlong JNICALL Java_dynamicmetronome_metronome_Program_createProgram(JNIEnv *env, jobject thiz) {
    return reinterpret_cast<jlong>(new Program);
}

extern "C" JNIEXPORT void JNICALL Java_dynamicmetronome_metronome_Program_destroyProgram(JNIEnv *env, jobject thiz, jlong handle) {
    delete reinterpret_cast<Program *>(handle);
}

extern "C" JNIEXPORT void JNICALL Java_dynamicmetronome_metronome_Program_addOrChangeInstruction(JNIEnv *env, jobject thiz, jlong handle, jlong bar, jint tempo, jboolean interpolate) {
    reinterpret_cast<Program *>(handle)->addOrChangeInstruction(bar, tempo, interpolate);
}

extern "C" JNIEXPORT jdoubleArray JNICALL Java_dynamicmetronome_metronome_Program_compile(JNIEnv *env, jobject thiz, jlong handle) {
    std::vector<double> *compiledInstructions{reinterpret_cast<Program *>(handle)->compile()};
    jdoubleArray output{env->NewDoubleArray((int) compiledInstructions->size())};
    env->SetDoubleArrayRegion(output, 0, (int) compiledInstructions->size(), compiledInstructions->data());
    return output;
}

extern "C" JNIEXPORT jlong JNICALL Java_dynamicmetronome_metronome_Program_length(JNIEnv *env, jobject thiz, jlong handle) {
    return static_cast<jlong>(reinterpret_cast<Program *>(handle)->length());
}

extern "C" JNIEXPORT void JNICALL Java_dynamicmetronome_metronome_Program_clear(JNIEnv *env, jobject thiz, jlong handle) {
    reinterpret_cast<Program *>(handle)->clear();
}
extern "C" JNIEXPORT jstring JNICALL Java_dynamicmetronome_metronome_Program_getName(JNIEnv *env, jobject thiz, jlong handle) {
    return env->NewStringUTF(reinterpret_cast<Program *>(handle)->getName().c_str());
}
extern "C" JNIEXPORT jstring JNICALL Java_dynamicmetronome_metronome_Program_serialize(JNIEnv *env, jobject thiz, jlong handle) {
    std::stringstream outputStream{};
    reinterpret_cast<Program *>(handle)->serialize(outputStream);
    return env->NewStringUTF(outputStream.str().c_str());
}
extern "C" JNIEXPORT jlong JNICALL Java_dynamicmetronome_metronome_Program_deserialize(JNIEnv *env, jobject thiz, jstring path) {
    auto *program{new Program()};
    std::ifstream file{};
    const char *filePath{env->GetStringUTFChars(path, nullptr)};
    file.open(filePath);
    env->ReleaseStringUTFChars(path, filePath);
    program->deserialize(file);
    file.close();
    return reinterpret_cast<jlong>(program);
}

extern "C" JNIEXPORT jlong JNICALL Java_dynamicmetronome_metronome_Program_getHighestTempo(JNIEnv *env, jobject thiz, jlong handle) {
    return static_cast<jlong>(reinterpret_cast<Program *>(handle)->getHighestTempo());
}

extern "C" JNIEXPORT jlong JNICALL Java_dynamicmetronome_metronome_Program_getLowestTempo(JNIEnv *env, jobject thiz, jlong handle) {
    return static_cast<jlong>(reinterpret_cast<Program *>(handle)->getLowestTempo());
}