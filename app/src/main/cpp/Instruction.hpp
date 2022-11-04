#pragma once

#include <cstdint>

class Instruction {
public:
    double startTempo;
    double tempoOffset;
    uint64_t beats;
};