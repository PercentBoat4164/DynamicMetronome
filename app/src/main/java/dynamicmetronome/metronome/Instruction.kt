package dynamicmetronome.metronome

import dynamicmetronome.mainactivity.STARTING_TEMPO
import java.io.Serializable

class Instruction : Serializable{
    var tempo = STARTING_TEMPO
    var interval = (60000F / tempo).toLong()
    var interpolate = false

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
