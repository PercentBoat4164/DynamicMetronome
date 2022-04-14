package thadd.schelp.dynamicmetronome.metronome

import thadd.schelp.dynamicmetronome.BEATS_PER_MEASURE
import thadd.schelp.dynamicmetronome.STARTING_TEMPO

import android.content.Context
import android.graphics.Color
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

/**
 * The Metronome class
 *
 * Plays sounds at a regular interval. The sound, interval, and whether or not it is currently playing can all be changed dynamically with helper functions.
 */
class Metronome() : Runnable {
    private lateinit var program: Program  // Attached Program

    private var handler = Handler(Looper.getMainLooper())  // handler used to manage timing
    private var soundID = 0  // ID of sound resource
    private var soundPool = SoundPool.Builder().build()  // sound pool used to play sounds
    private var playHead = 0  // playHead of program
    private var series = LineGraphSeries(mutableListOf(DataPoint(0.0, 0.0), DataPoint(0.0, 100.0)).toTypedArray())  // series used to update GraphView
    private var graph: GraphView? = null  // GraphView to display playHead on
    private var playing = false  // is the metronome playing

    var tempo = STARTING_TEMPO  // current tempo of metronome
    var volume = 100f  // volume of metronome

    /**
     * Creates a Metronome with a specific sound and tempo.
     * @param tempo Initial tempo. Default: STARTING_TEMPO
     * @param soundID Initial soundID
     */
    constructor(tempo: Int=STARTING_TEMPO, soundID: Int) : this() {
        series.setAnimated(true)
        this.tempo = tempo
        this.soundID = soundID
    }

    /**
     * This default constructor ensures that the graph series will be animated.
     */
    init {
        series.setAnimated(true)
    }

    /**
     * Starts playing the Metronome
     */
    private fun start() {
        playing = true
        handler.removeCallbacks(this)  // clear any queued plays
        handler.postDelayed(this, 100)  // queue a new play for right now
    }

    /**
     * Stop playing the Metronome
     */
    fun stop() {
        playing = false
        handler.removeCallbacks(this)  // Clear any queued plays
    }

    /**
     * Toggle playing the Metronome. If the Metronome is playing, stop. Otherwise, start playing.
     */
    fun togglePlaying() {
        if (!playing) {  // If stopped,
            start()
        }
        else {
            stop()
        }
    }

    /**
     * Set the soundID. This can be done on creation with the Metronome(Int, Int) constructor.
     * The soundID controls which sound the Metronome uses for its clicks.
     * @param context The application context
     * @param sound The sound to use
     */
    fun setSoundID(context: Context, sound: Int) {
        this.soundID = soundPool.load(context, sound, 1)
    }

    override fun run() {
        if (playing) {  // if playing normal metronome
            handler.postDelayed(this, (60000F / tempo).toLong())
            soundPool.play(soundID, volume, volume, 0, 0, 1F)
        }
        else {  // if running a program
            if (program.length() > playHead) {
                handler.postDelayed(this, program.getInstruction(playHead).toLong())
                soundPool.play(soundID, volume, volume, playHead, 0, 1F)  // use playHead as priority so that each beat has a higher priority than the last.

                // update graph to show new series
                graph?.removeSeries(series)
                series = LineGraphSeries(mutableListOf(DataPoint(playHead.toDouble() / BEATS_PER_MEASURE + 1, 0.0), DataPoint(playHead.toDouble() / BEATS_PER_MEASURE + 1, 1 / program.getInstruction(playHead) * 60000)).toTypedArray())
                series.color = Color.GREEN
                graph?.addSeries(series)

                // increment the playHead
                ++playHead
            }
            else {
                graph?.removeSeries(series)
                playHead = 0
            }
        }
    }

    /**
     * Executes the program that was passed to it. The program must be compiled before hand.
     * @param program The program to execute
     */
    fun executeProgram(program: Program, graph: GraphView?) {
        this.program = program
        handler.removeCallbacks(this)
        handler.post(this)
        if (graph != null) {
            this.graph = graph
        }
    }
}