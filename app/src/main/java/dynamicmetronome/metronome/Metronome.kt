package dynamicmetronome.metronome

import android.content.Context
import android.graphics.Color
import android.media.SoundPool
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import dynamicmetronome.mainactivity.BEATS_PER_MEASURE
import dynamicmetronome.mainactivity.STARTING_QUARTER_VOLUME
import dynamicmetronome.mainactivity.STARTING_TEMPO
import kotlin.system.measureNanoTime

/**
 * The Metronome class
 *
 * Plays sounds at a regular interval. The sound, interval, and whether or not it is currently playing can all be changed dynamically with helper functions.
 */
class Metronome() : Runnable {
    private var priority = 1
    private var thread = Thread(this)
    private var soundID = 0  // ID of sound resource
    private var soundPool = SoundPool.Builder().setMaxStreams(10).build()  // sound pool used to play sounds
    private var playHead = 0  // playHead of program
    private var series = LineGraphSeries(mutableListOf(DataPoint(0.0, 0.0), DataPoint(0.0, 100.0)).toTypedArray())  // series used to update GraphView
    private var graph: GraphView? = null  // GraphView to display programs on
    var program = Program()  // Attached Program
    var playing = false  // is the metronome playing
    var tempo = STARTING_TEMPO  // current tempo of metronome
    var volume = STARTING_QUARTER_VOLUME  // volume of metronome

    /**
     * Creates a Metronome with a specific sound and tempo.
     * @param graph overrides the graph
     */
    constructor(graph: GraphView?, context: Context, sound: Int) : this() {
        this.graph = graph
        soundID = soundPool.load(context, sound, Int.MAX_VALUE)
    }

    /**
     * Starts playing the Metronome
     */
    fun start() {
        stop()
        playing = true
        thread.start()
    }

    /**
     * Stop playing the Metronome
     */
    fun stop() {
        playing = false
        if (thread.isAlive) {
            thread.join()
        }
        thread = Thread(this)
        thread.priority = Thread.MAX_PRIORITY
        priority = 0
        playHead = 0
    }

    /**
     * Toggle playing the Metronome. If the Metronome is playing, stop. Otherwise, start playing.
     */
    fun togglePlaying() {
        if (!playing) {
            start()
        } else {
            stop()
        }
    }

    override fun run() {
//        series = LineGraphSeries(mutableListOf(DataPoint(playHead.toDouble() / BEATS_PER_MEASURE, 0.0), DataPoint(playHead.toDouble() / BEATS_PER_MEASURE, 1 / program.getInstruction(playHead) * 60000)).toTypedArray())
        series.color = Color.GREEN
        graph?.addSeries(series)
        while (playing) {
            if (playHead < program.length()) {
                Thread.sleep(kotlin.math.max((program.getInstruction(playHead) - measureNanoTime {
                    soundPool.play(soundID, volume, volume, playHead, 0, 1F)
                    series.resetData(mutableListOf(DataPoint(playHead.toDouble() / BEATS_PER_MEASURE, 0.0), DataPoint(playHead.toDouble() / BEATS_PER_MEASURE, 1 / program.getInstruction(playHead) * 60000)).toTypedArray())
                    ++playHead
                } / 1000000F).toLong(), 0))
            }
            else if (playHead == program.length() && program.instructions.isNotEmpty()) {
                playing = false
                graph?.removeSeries(series)
                playHead = 0
                break
            }
            else {
                 Thread.sleep(kotlin.math.max((60000F / tempo - measureNanoTime {
                     soundPool.play(soundID, volume, volume, priority, 0, 1F)
                 } / 1000000F).toLong(), 0))
            }
        }
    }

    /**
     * Executes the program that the metronome already has.
     */
    fun executeProgram() {
        program.compile()
        if (program.instructions.isNotEmpty()) {
            graph?.removeAllSeries()
            graph?.addSeries(LineGraphSeries(program.setGraph().toTypedArray()))
            formatGraph()
        }
        start()
    }

    fun formatGraph() {
        graph?.viewport?.setMaxY(program.highestTempo + program.highestTempo * .02)  // Highest tempo + 2% leaves a comfortable space
        graph?.viewport?.setMinY(program.lowestTempo - program.lowestTempo * .02)  // Same here.
        if (program.numBars != 0) {
            graph?.viewport?.setMaxX(program.numBars.toDouble())
        }
    }
}