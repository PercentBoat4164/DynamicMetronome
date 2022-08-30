#ifndef DYNAMIC_METRONOME_PROGRAM_HPP
#define DYNAMIC_METRONOME_PROGRAM_HPP

#include "Instruction.hpp"

#include <string>
#include <jni.h>
#include <map>
#include <vector>
#include <ostream>

class Program {
private:
    uint64_t m_highestTempo{0};
    uint64_t m_lowestTempo{UINT64_MAX};
    size_t m_numBars;
    std::map<size_t, Instruction> m_instructions;
    std::string m_name;

public:
    void addOrChangeInstruction(size_t, uint64_t, bool);
    std::vector<double> *compile();
    size_t length() const;
    void clear();
    void serialize(std::ostream &) const;
    Program *deserialize(std::istream &);
    std::string getName();
    std::map<size_t, Instruction> *getInstructions();
    uint64_t getHighestTempo() const;
    uint64_t getLowestTempo() const;

    friend std::ostream &operator<<(std::ostream &os, const Program &program);
    friend std::istream &operator>>(std::istream &os, Program &program);
};


#endif //DYNAMIC_METRONOME_PROGRAM_HPP
