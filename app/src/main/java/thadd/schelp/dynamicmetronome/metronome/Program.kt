package thadd.schelp.dynamicmetronome.metronome

import thadd.schelp.dynamicmetronome.BEATS_PER_MEASURE

/**
 * The Program class
 *
 * Contains a list of instructions which can be parsed by a Metronome to simulate accelerandos, or hold the tempo steady. Instructions can be added, changed, or removed. Programs must be compile before they can be executed by a Metronome.
 */
class Program() {
    // list of compiled instructions. These take the form of a Long containing the number of milliseconds to wait between this beat and the next one. When time signatures are added, a new instruction format will be needed.
    private var compiledInstructions = mutableListOf<Long>()

    // a hash table of bar numbers to instructions.
    var instructions = mutableMapOf<Int, Instruction>()

    /**
     *
     */
    constructor(byteArray: ByteArray) : this() {
        setData(byteArray)
    }

    fun addOrChangeInstruction(bar: Int, tempo: Int, interpolate: Boolean): Program {
        if (tempo > 0) {
            instructions[(bar - 1).coerceAtLeast(0)] = Instruction().setTempo(tempo).setInterpolation(interpolate)
        }
        else {
            instructions.remove(bar-1)
        }
        return this
    }

    // FIXME: The compilation algorithm is broken.
    fun compile(): Program {
        compiledInstructions = mutableListOf()
        val instructions = instructions.toSortedMap().toList()
        var tempo = instructions[0].second.tempo
        var count = 0
        for (instruction in instructions.indices) {
            tempo = instructions[instruction].second.tempo
            if (tempo <= 0) {
                break
            }
            try {
                for (barNumber in (instructions[instruction].first + count)..instructions[instruction + 1].first) {
                    for (beatNumber in (barNumber * 4) until (barNumber * 4) + BEATS_PER_MEASURE) {
                        if (instructions[instruction + 1].second.interpolate) {
                            tempo += (instructions[instruction + 1].second.tempo - instructions[instruction].second.tempo) / (BEATS_PER_MEASURE * (instructions[instruction + 1].first - instructions[instruction].first))
                        }
                        compiledInstructions.add((60000 / tempo).toLong())
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                break
            }
            count ++
        }
        // This instruction adds a down beat to the end of the program
        compiledInstructions.add((60000 / tempo).toLong())
        return this
    }

    fun getData(): ByteArray {
        var byteArray = byteArrayOf()
        val instructions = instructions.toSortedMap().toList()
        for (instruction in instructions) {
            byteArray += instruction.first.toByte()
            byteArray += instruction.second.tempo.toByte()
            byteArray += (if (instruction.second.interpolate) 1 else 0).toByte()
        }
        return byteArray
    }

    fun setData(byteArray: ByteArray): Program {
        compiledInstructions = mutableListOf()
        instructions = mutableMapOf()
        for (byte in byteArray.indices step 3) {
            addOrChangeInstruction(byteArray[byte].toInt(), byteArray[byte + 1].toInt(), byteArray[byte + 2].toInt() == 1)
        }
        return this
    }

    fun getInstruction(position: Int): Long {
        return compiledInstructions[position]
    }

    fun length(): Int {
        return compiledInstructions.size
    }
}