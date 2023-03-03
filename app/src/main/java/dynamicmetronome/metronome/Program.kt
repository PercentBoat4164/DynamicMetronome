package dynamicmetronome.metronome

import java.io.Serializable
import java.util.*

class Program : Serializable {
    private var instructions: SortedMap<Long, Instruction> = sortedMapOf()
    var name = ""

    fun addOrChangeInstruction(bar: Long, tempo: Double, interpolation: Boolean) {
        // The bar cannot be less than 0.
        if (bar < 0) throw NegativeArraySizeException()
        // Tempo of 0 means delete instruction.
        if (tempo == 0.0 && bar != 0L) instructions.remove(bar)
        else instructions[bar] = Instruction(tempo, interpolation)
        if (!instructions.containsKey(0)) instructions[0] = instructions[bar]?.copy()
    }

    fun getTempos(): DoubleArray {
        val tempos = ArrayList<Double>()
        for (instruction in instructions.keys.zipWithNext()) {
            var start = instructions[instruction.first]!!.tempo
            if (instructions[instruction.second]!!.interpolation) {
                val end = instructions[instruction.second]!!.tempo
                val beats = 4.0 * (instruction.second - instruction.first)  // When adding time signatures, this should come from the first instruction
                val offset = (end - start) / beats
                for (beat in 1..beats.toInt()) {
                    tempos.add(start)
                    start += offset
                }
            } else {
                val beats = 4.0 * (instruction.second - instruction.first)  // When adding time signatures, this should come from the first instruction
                for (beat in 1..beats.toInt()) tempos.add(start)
            }
        }
        return tempos.toDoubleArray()
    }

    // This will include no elements or at least one element that has a key of 0.
    fun getInstructions(): SortedMap<Long, Instruction> {
        return instructions
    }
}