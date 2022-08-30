package dynamicmetronome.metronome

import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.io.Closeable
import kotlin.math.max

/**
 * Metronome wrapper class
 *
 * Contains a handle to the C++ object data that is used by this class on the C++ side.
 */
class Metronome : Closeable {
    private val handle: Long = createMetronome()

    private external fun createMetronome() : Long
    private external fun destroyMetronome(handle: Long)

    fun start() = start(handle)
    fun stop() = stop(handle)
    fun executeProgram() = executeProgram(handle)
    fun togglePlaying() = togglePlaying(handle)
    fun getProgram() = Program(getProgram(handle))
    fun loadProgram(path: String) = Program(loadProgram(handle, path))
    fun setTempo(tempo: Int) = setTempo(handle, tempo)
    fun setVolume(volume: Double) = setVolume(handle, volume)
    fun getGraphContents() = getGraphContents(handle)
    fun updateGraph(graph: GraphView) {
        val graphData = getGraphContents()
        val graphArray = mutableListOf<DataPoint>()
        for (i in graphData.indices step(2)) graphArray.add(DataPoint(graphData[i + 1], graphData[i]))  // Create graph series data from graph contents from C++
        graph.removeAllSeries()
        graph.addSeries(LineGraphSeries(graphArray.toTypedArray()))

        // Set graph viewport dimensions correctly
        graph.viewport.setMaxY(getProgram().getHighestTempo() + getProgram().getHighestTempo() * .02)  // Highest tempo + 2% leaves a comfortable space
        graph.viewport.setMinY(getProgram().getLowestTempo() - getProgram().getLowestTempo() * .02)  // Same here.
        graph.viewport.setMaxX(max(graphData[graphData.size-1] + 1, 1.0))
        graph.viewport.setMinX(0.0)
    }

    private external fun start(handle: Long)
    private external fun stop(handle: Long)
    private external fun executeProgram(handle: Long)
    private external fun togglePlaying(handle: Long)
    private external fun getProgram(handle: Long) : Long
    private external fun loadProgram(handle: Long, path: String) : Long
    private external fun setTempo(handle: Long, tempo: Int)
    private external fun setVolume(handle: Long, volume: Double)
    private external fun getGraphContents(handle: Long) : DoubleArray

    // Destructor
    override fun close() = destroyMetronome(handle)
}
