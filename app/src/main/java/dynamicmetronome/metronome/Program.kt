package dynamicmetronome.metronome

import com.jjoe64.graphview.series.DataPoint
import dynamicmetronome.activities.MainActivity
import java.io.Serializable

/**
 * The Program class
 *
 * Contains a list of instructions which can be parsed by a Metronome to simulate accelerandos, or hold the tempo steady. Instructions can be added, changed, or removed. Programs must be compile before they can be executed by a Metronome.
 */
class Program(
    // list of compiled instructions. These take the form of a Long containing the number of milliseconds to wait between this beat and the next one. When time signatures are added, a new instruction format will be needed.
    private var compiledInstructions: MutableList<Double> = mutableListOf(),
    var highestTempo: Double = 2.0,
    var lowestTempo: Double = Double.MAX_VALUE,
    var numBars: Int = 0,
    var instructions: MutableMap<Int, Instruction> = mutableMapOf(),
    var name: String = ""
) : Serializable {

    fun addOrChangeInstruction(bar: Int, tempo: Int, interpolate: Boolean): Program {
        val realBar = bar.coerceAtLeast(0)
        if (tempo > 0) {
            instructions[realBar] = Instruction().setTempo(tempo).setInterpolation(interpolate)
        }
        else {
            instructions.remove(bar)
        }
        numBars = kotlin.math.max(realBar, numBars)
        highestTempo = kotlin.math.max(tempo.toDouble(), highestTempo)
        lowestTempo = kotlin.math.min(tempo.toDouble(), lowestTempo)
        return this
    }

    fun compile(): Program {
        if (instructions.isEmpty()) {
            return this
        }
        compiledInstructions = mutableListOf()
        val instructions = instructions.toSortedMap().toList()
        var tempo = instructions[0].second.tempo.toDouble()
        for (instruction in instructions.indices) {
            tempo = instructions[instruction].second.tempo.toDouble()
            try {
                val slope = (instructions[instruction + 1].second.tempo - instructions[instruction].second.tempo) / (MainActivity.BEATS_PER_MEASURE * (instructions[instruction + 1].first - instructions[instruction].first))
                for (barNumber in (instructions[instruction].first) until instructions[instruction + 1].first) {
                    for (beatNumber in 0 until MainActivity.BEATS_PER_MEASURE.toInt()) {
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

    fun getInstruction(position: Int): Double {
        return compiledInstructions[position]
    }

    fun length(): Int {
        return compiledInstructions.size
    }

    fun clear() {
        instructions.clear()
        compiledInstructions.clear()
        highestTempo = 0.0
        lowestTempo = Double.MAX_VALUE
        numBars = 0
        name = ""
    }

    fun setGraph() : MutableList<DataPoint> {
        val graphArray = mutableListOf<DataPoint>()
        var tempo = 0
        val instructions = instructions.toSortedMap().toList()
        for (i in instructions) {
            if (!i.second.interpolate) {
                graphArray.add(DataPoint(i.first.toDouble(), tempo.toDouble()))
            }
            graphArray.add(DataPoint(i.first.toDouble(), i.second.tempo.toDouble()))
            tempo = i.second.tempo
        }
        return graphArray
    }
}