package thadd.schelp.dynamicmetronome

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileNotFoundException
import java.lang.IndexOutOfBoundsException

class MetronomeProgram(attachToMetronome: Metronome, appContext: Context) {
    var states = mutableMapOf<Int, MetronomeState>()
    var metronome = attachToMetronome
    var compiledInstructions = mutableListOf<Long>()
    private var context = appContext
    var name = ""

    fun addOrChangeInstruction(bar: Int, tempo: Int, interpolate: Boolean): MetronomeProgram {
        if (tempo > 0) { states[(bar - 1).coerceAtLeast(0)] = MetronomeState().setTempo(tempo).setInterpolation(interpolate) } else { states.remove(bar-1) }
        return this
    }

    fun compile(): MetronomeProgram{
        compiledInstructions = mutableListOf()
        val instructions = states.toSortedMap().toList()
        var tempo = instructions[0].second.tempo
        var count = 0
        for (instruction in instructions.indices) {
            tempo = instructions[instruction].second.tempo
            if (tempo <= 0) { break }
            try {
                for (barNumber in (instructions[instruction].first + count)..instructions[instruction + 1].first) {
                    for (beatNumber in (barNumber * 4) until (barNumber * 4) + BEATS_PER_MEASURE) {
                        if (instructions[instruction + 1].second.interpolate) {
                            tempo += (instructions[instruction + 1].second.tempo - instructions[instruction].second.tempo) / (BEATS_PER_MEASURE * (instructions[instruction + 1].first - instructions[instruction].first))
                            compiledInstructions.add((60000 / tempo).toLong())
                        } else { compiledInstructions.add((60000 / tempo).toLong()) }
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                break
            }
            count ++
        }
        compiledInstructions.add((60000 / tempo).toLong())
        return this
    }

    fun execute() {
        metronome.kill()
        metronome.attachProgram(compiledInstructions)
        metronome.start()
    }

    fun save(filename: String): MetronomeProgram {
        var byteArray = byteArrayOf()
        val instructions = states.toSortedMap().toList()
        for (instruction in instructions) {
            byteArray += instruction.first.toByte()
            byteArray += instruction.second.tempo.toByte()
            byteArray += (if (instruction.second.interpolate) 1 else 0).toByte()
        }
        val file = File(context.filesDir, "$filename.met")
        try { file.delete() } catch (e: FileNotFoundException) {}
        file.createNewFile()
        file.writeBytes(byteArray)
        return this
    }

    fun load(filename: String): MetronomeProgram {
        states = mutableMapOf()
        try {
            val byteArray = File(context.filesDir, "$filename.met").readBytes()
            for (byte in byteArray.indices step 3) { addOrChangeInstruction(byteArray[byte].toInt(), byteArray[byte + 1].toInt(), byteArray[byte + 2].toInt() == 1) }
        } catch (e: FileNotFoundException) {}
        return this
    }
}

class MetronomeState {
    var tempo = STARTING_TEMPO
    var soundPool: SoundPool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN).setUsage(AudioAttributes.USAGE_UNKNOWN).build()).build()
    var volume = 1F
    var interval = (60000F / tempo).toLong()
    var interpolate = false

    @JvmName("setTempo1")
    fun setTempo(newTempo: Int) : MetronomeState {
        tempo = newTempo
        interval = (60000F / tempo).toLong()
        return this
    }

    @JvmName("setVolume1")
    fun setVolume(newVolume: Float) : MetronomeState {
        volume = newVolume
        return this
    }

    fun setInterpolation(interpolation: Boolean): MetronomeState {
        interpolate = interpolation
        return this
    }
}

class Metronome(metronomeState: MetronomeState, appContext: Context) : Runnable {
    private var context = appContext
    private var paused = true
    private var handler = Handler(Looper.getMainLooper())
    private var soundID = 0
    private var playHead = 0
    private var program = mutableListOf<Long>()
    var state = metronomeState

    fun start() {
        paused = false
        handler.removeCallbacks(this)
        handler.post(this)
    }

    fun kill() {
        paused = true
        handler.removeCallbacks(this)
    }

    fun togglePlaying() {
        if (paused) { start() } else { kill() }
    }

    fun setVolume(volume: Float) {
        state.setVolume(volume)
    }

    fun generateSoundIDs() {
        soundID = state.soundPool.load(context, R.raw.beep, 1)
    }

    fun attachProgram(compiledInstructions: MutableList<Long>) {
        program = compiledInstructions
    }

    override fun run() {
        if (program.isNotEmpty()) {
            try {
                if (!paused) {
                    handler.postDelayed(this, program[playHead] - 1)
                    state.soundPool.play(soundID, state.volume, state.volume, playHead, 0, 1F)
                    playHead++
                }
            } catch (e: IndexOutOfBoundsException) {
                playHead = 0
            }
        } else {
            handler.postDelayed(this, state.interval - 1)
            if (!paused) { state.soundPool.play(soundID, state.volume, state.volume, 0, 0, 1F) }
            return
        }
        if (program.isEmpty()) {
            playHead = 0
            program.clear()
        }
    }
}