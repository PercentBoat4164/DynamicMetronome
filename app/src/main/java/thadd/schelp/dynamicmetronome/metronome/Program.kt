package thadd.schelp.dynamicmetronome.metronome

import thadd.schelp.dynamicmetronome.BEATS_PER_MEASURE

/**
 * The Program class
 *
 * Contains a list of instructions which can be parsed by a Metronome to simulate accelerandos, or hold the tempo steady. Instructions can be added, changed, or removed. Programs must be compile before they can be executed by a Metronome.
 */
class Program {
    // list of compiled instructions. These take the form of a Long containing the number of milliseconds to wait between this beat and the next one. When time signatures are added, a new instruction format will be needed.
    private var compiledInstructions = mutableListOf<Double>()
    var highestTempo = 0.0
    var lowestTempo = Double.MAX_VALUE
    var numBars = 0

    // a hash table of bar numbers to instructions.
    var instructions = mutableMapOf<Int, Instruction>()

    fun addOrChangeInstruction(bar: Int, tempo: Int, interpolate: Boolean): Program {
        if (tempo > 0) {
            instructions[(bar).coerceAtLeast(0)] = Instruction().setTempo(tempo).setInterpolation(interpolate)
        }
        else {
            instructions.remove(bar)
        }
        numBars = kotlin.math.max(bar, numBars)
        highestTempo = kotlin.math.max(tempo.toDouble(), highestTempo)
        lowestTempo = kotlin.math.min(tempo.toDouble(), lowestTempo)
        return this
    }

    fun compile(): Program {
        compiledInstructions = mutableListOf()
        val instructions = instructions.toSortedMap().toList()
        var tempo = instructions[0].second.tempo.toDouble()
        for (instruction in instructions.indices) {
            tempo = instructions[instruction].second.tempo.toDouble()
            try {
                val slope = (instructions[instruction + 1].second.tempo - instructions[instruction].second.tempo) / (BEATS_PER_MEASURE * (instructions[instruction + 1].first - instructions[instruction].first))
                for (barNumber in (instructions[instruction].first) until instructions[instruction + 1].first) {
                    for (beatNumber in 0 until BEATS_PER_MEASURE.toInt()) {
                        compiledInstructions.add(60000.0 / tempo)
                        if (instructions[instruction + 1].second.interpolate) {
                            tempo += slope
                        }
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                break
            }
        }
        // This instruction adds a down beat to the end of the program
        compiledInstructions.add((60000.0 / tempo))
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

    fun getInstruction(position: Int): Double {
        return compiledInstructions[position]
    }

    fun length(): Int {
        return compiledInstructions.size
    }
}