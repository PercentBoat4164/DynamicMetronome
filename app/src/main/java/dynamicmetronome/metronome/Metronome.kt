package dynamicmetronome.metronome

import android.content.Context
import android.graphics.Color
import android.media.SoundPool
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import dynamicmetronome.activities.MainActivity
import kotlin.system.measureNanoTime

/**
 * The Metronome class
 *
 * Plays sounds at a regular interval. The sound, interval, and whether or not it is currently playing can all be changed dynamically with helper functions.
 */
class Metronome (
    private var priority: Int = 1,
    private var thread: Thread? = null,
    private var soundPool: SoundPool = SoundPool.Builder().setMaxStreams(10).build(),  // sound pool used to play sounds
    private var series: LineGraphSeries<DataPoint> = LineGraphSeries(mutableListOf(DataPoint(0.0, 0.0), DataPoint(0.0, 100.0)).toTypedArray()),  // series used to update GraphView
    var graph: GraphView? = null,  // GraphView to display programs on
    private var soundID: Int = 0,  // ID of sound resource
    private var playHead: Int = 0,  // playHead of program
    var program: Program = Program(),  // Attached Program
    var playing: Boolean = false,  // is the metronome playing
    var tempo: Int = MainActivity.STARTING_TEMPO,  // current tempo of metronome
    var volume: Float = MainActivity.STARTING_QUARTER_VOLUME,  // volume of metronome
) : Runnable {

    /**
     * Initializes values that cannot be initialized in the primary constructor due to the object's lack of existence
     */
    init {
        thread = Thread(this)
    }

    /**
     * Creates a Metronome with a specific sound and tempo.
     * @param graph overrides the graph
     */
    constructor(graph: GraphView?, context: Context, sound: Int) : this() {
        thread = Thread(this)
        this.graph = graph
        soundID = soundPool.load(context, sound, 1)
    }

    /**
     * Starts playing the Metronome
     */
    private fun start() {
        stop()
        playing = true
        thread?.start()
    }

    /**
     * Stop playing the Metronome
     */
    fun stop() {
        playing = false
        if (thread?.isAlive!!) {
            thread?.join()
        }
        thread = Thread(this)
        thread!!.priority = Thread.MAX_PRIORITY
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
        series.color = Color.GREEN
        graph?.addSeries(series)
        while (playing) {
            if (playHead < program.length()) {
                Thread.sleep(kotlin.math.max((program.getInstruction(playHead) - measureNanoTime {
                    soundPool.play(soundID, volume, volume, playHead, 0, 1F)
                    series.resetData(mutableListOf(DataPoint(playHead.toDouble() / MainActivity.BEATS_PER_MEASURE, 0.0), DataPoint(playHead.toDouble() / MainActivity.BEATS_PER_MEASURE, 1 / program.getInstruction(playHead) * 60000)).toTypedArray())
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
            updateGraph()
        }
        start()
    }

    /**
     * Formats the graph to fit the entire area used by the program.
     */
    fun formatGraph() {
        graph?.viewport?.setMaxY(program.highestTempo + program.highestTempo * .02)  // Highest tempo + 2% leaves a comfortable space
        graph?.viewport?.setMinY(program.lowestTempo - program.lowestTempo * .02)  // Same here.
        if (program.numBars != 0) {
            graph?.viewport?.setMaxX(program.numBars.toDouble())
        }
    }

    fun updateGraph() {
        graph?.removeAllSeries()
        if (program.instructions.isNotEmpty()) graph?.addSeries(series)
        graph?.addSeries(LineGraphSeries(program.setGraph().toTypedArray()))
        formatGraph()
    }
}