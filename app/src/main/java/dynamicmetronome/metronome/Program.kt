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
              if (interpolation) Double.POSITIVE_INFINITY else 0.0,
              Long.MAX_VALUE)
        }
    }

    fun getTempos(): DoubleArray {
        val result = ArrayList<Double>()
        val entries = getInstructionsAndBars()
        var tempo = entries[0].second.startTempo
        for (i in 0 until entries.size) {
            for (j in 0 until entries[i].second.beats.toInt()) {
                result.add(tempo)
                tempo += entries[i].second.tempoOffset
            }
        }
        return result.toTypedArray().toDoubleArray()
    }

    fun getInstructionsAndBars(): ArrayList<Pair<Long, Instruction>> {
        // Force Kotlin to perform a deep copy. I hate you Java! Why must you always pass by reference?
        val temp: MutableMap<Long, Instruction> = HashMap()
        instructions.forEach{(key, value) -> temp[key] = Instruction(value.startTempo, value.tempoOffset, value.beats)}
        val entries = ArrayList(temp.toSortedMap().toList())
        if (entries.size > 0) {  // If instructions exist, build them.
            if (entries[0].first != 0L) entries.add(0, Pair(0, Instruction(entries[0].second.startTempo, 0.0, 0)))  // 4 as in 4/4 time signature
            for (entry in 1 until entries.size) {
                entries[entry - 1].second.beats = (entries[entry].first - entries[entry - 1].first) * 4  // 4 as in 4/4 time signature
                if (entries[entry].second.tempoOffset != 0.0)
                    entries[entry - 1].second.tempoOffset = (entries[entry].second.startTempo - entries[entry - 1].second.startTempo) / (entries[entry - 1].second.beats)
            }
            entries.last().second.beats = 1
            entries.last().second.tempoOffset = 0.0
        }
        return entries
    }

    fun clear() {
        instructions.clear()
        name = ""
    }
}