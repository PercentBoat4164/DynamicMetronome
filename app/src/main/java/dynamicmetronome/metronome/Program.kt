package dynamicmetronome.metronome

data class Instruction(val startTempo: Double, var tempoOffset: Double, var beats: Long)

class Program {
    private var instructions: MutableMap<Long, Instruction> = mutableMapOf()
    var name = ""

    fun addOrChangeInstruction(bar: Long, tempo: Double, interpolation: Boolean) {
        if (tempo == 0.0) instructions.remove(bar)
        else {
            instructions[bar] = Instruction(
              tempo,
              if (interpolation) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY,
              Long.MAX_VALUE)
        }
    }

    fun getTempos(): DoubleArray {
        val result = ArrayList<Double>()
        val entries = getInstructionsAndBars()
        var tempo = entries[0].second.startTempo
        for (entry in entries) {
            for (i in 0 until entry.second.beats) {
                result.add(tempo)
                tempo += entry.second.tempoOffset
            }
        }
        return result.toTypedArray().toDoubleArray()
    }

    fun getInstructionsAndBars(): ArrayList<Pair<Long, Instruction>> {
        val entries = ArrayList(instructions.toSortedMap().toList())
        if (entries.size > 0) {  // If instructions exist, build them.
            entries.add(0, Pair(0, Instruction(entries[0].second.startTempo, 0.0, entries[0].first * 4)))  // 4 as in 4/4 time signature
            for (entry in 1 until entries.size) {
                try {
                    entries[entry].second.beats = (entries[entry].first - entries[entry - 1].first) * 4  // 4 as in 4/4 time signature
                    if (entries[entry].second.tempoOffset == Double.POSITIVE_INFINITY)
                        entries[entry].second.tempoOffset = entries[entry].second.startTempo - entries[entry - 1].second.startTempo
                    else entries[entry].second.tempoOffset = 0.0
                } catch (_: java.lang.IndexOutOfBoundsException) {
                    entries[entry].second.beats = entries[entry].first * 4  // 4 as in 4/4 time signature
                    if (entries[entry].second.tempoOffset == Double.POSITIVE_INFINITY)
                        entries[entry].second.tempoOffset = entries[entry].second.startTempo
                    else entries[entry].second.tempoOffset = 0.0
                }
            }
            entries.last().second.beats = 1
        }
        return entries
    }

    fun clear() {
        instructions.clear()
        name = ""
    }
}