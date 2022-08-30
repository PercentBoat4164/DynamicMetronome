#ifndef DYNAMIC_METRONOME_INSTRUCTION_HPP
#define DYNAMIC_METRONOME_INSTRUCTION_HPP

#include <cstdint>
#include <ostream>
#include <istream>

class Instruction {
public:
    Instruction();
    Instruction(uint64_t t_tempo, uint64_t m_notesPerMeasure, uint64_t m_typeOfNote, bool t_interpolate, size_t t_barNumber);

    void setTempo(uint64_t);
    void setInterpolation(bool);

    size_t getTempo() const;
    size_t getBarNumber() const;

    uint64_t getNotesPerMeasure() const;

    bool getInterpolation();

public:
    friend std::ostream &operator<<(std::ostream &, const Instruction &);
    friend std::istream &operator>>(std::istream &, Instruction &);

private:
    // Time signature: 4:4: four quarter notes per bar
    uint64_t m_notesPerMeasure{4};  // 4 notes
    uint64_t m_typeOfNote{4};  // Quarter Note
    bool m_interpolate{};

    uint64_t m_tempo{};

    // Location in program
    size_t m_barNumber{};

    Instruction *deserialize(std::istream &inputStream);

    void serialize(std::ostream &outputStream) const;
};


#endif //DYNAMIC_METRONOME_INSTRUCTION_HPP
