package dynamicmetronome.metronome

import java.io.Serializable

data class Instruction(var tempo: Double, var interpolation: Boolean) : Serializable
