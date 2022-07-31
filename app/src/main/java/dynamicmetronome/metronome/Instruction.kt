package dynamicmetronome.metronome

import dynamicmetronome.activities.MainActivity
import java.io.Serializable

class Instruction(
    var tempo: Int = MainActivity.STARTING_TEMPO,
    var interpolate: Boolean = false,
    private var interval: Long = (60000F / tempo).toLong(),
) : Serializable {

    fun setTempo(newTempo: Int) : Instruction {
        tempo = newTempo
        interval = (60000F / tempo).toLong()
        return this
    }

    fun setInterpolation(interpolation: Boolean): Instruction {
        interpolate = interpolation
        return this
    }
}
