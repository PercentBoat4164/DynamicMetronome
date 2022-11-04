package dynamicmetronome.metronome

data class Instruction(val startTempo: Double, val tempoOffset: Double, val beats: Long)

class Program {
    var name = ""
    private var highestTempo = 0.0
    private var lowestTempo = Double.MAX_VALUE
    private var instructions = mutableMapOf(Pair(0L, Instruction(130.0, 0.0, 0L)))

    fun addOrChangeInstruction(bar: Long, tempo: Double, interpolation: Boolean) {
        if (tempo == 0.0) instructions.remove(bar)
        else {
            instructions[bar] = Instruction(
              tempo,
              if (interpolation) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY,
              Long.MAX_VALUE)
            if (tempo > highestTempo) highestTempo = tempo
            else if (tempo < lowestTempo) lowestTempo = tempo
        }
    }

    fun clear() {
        highestTempo = 0.0
        lowestTempo = Double.MAX_VALUE
        instructions = mutableMapOf(Pair(0L, Instruction(130.0, 0.0, 0L)))
        name = ""
    }
}