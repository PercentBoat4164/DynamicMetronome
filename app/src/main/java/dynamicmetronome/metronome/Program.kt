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
        if (tempo == 0.0) instructions.remove(bar)
        instructions[bar] = Instruction(tempo, interpolation)
        if (!instructions.containsKey(0)) instructions[0] = instructions[bar]?.copy()
    }

    fun getTempos(): DoubleArray {
        val tempos = DoubleArray(instructions.size)
        var index = 0
        for (i in instructions) tempos[index++] = i.value.tempo
        return tempos
    }

    // This will include no elements or at least one element that has a key of 0.
    fun getInstructions(): SortedMap<Long, Instruction> {
        return instructions
    }

    fun clear() {
        instructions.clear()
        name = ""
    }

    fun length(): Int {
        return instructions.size
    }
}