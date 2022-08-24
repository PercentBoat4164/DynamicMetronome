package dynamicmetronome.metronome

import com.jjoe64.graphview.GraphView
import java.io.Closeable

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
    fun updateGraph(graph: GraphView) {
        /*
        fun setGraph() : MutableList<DataPoint> {
            val graphArray = mutableListOf<DataPoint>()
            var tempo = 0
            val m_instructions = m_instructions.toSortedMap().toList()
            for (i in m_instructions) {
                if (!i.second.interpolate) {
                    graphArray.add(DataPoint(i.first.toDouble(), tempo.toDouble()))
                }
                graphArray.add(DataPoint(i.first.toDouble(), i.second.tempo.toDouble()))
                tempo = i.second.tempo
            }
            return graphArray
        }
        */
    }

    private external fun start(handle: Long)
    private external fun stop(handle: Long)
    private external fun executeProgram(handle: Long)
    private external fun togglePlaying(handle: Long)
    private external fun getProgram(handle: Long) : Long
    private external fun loadProgram(handle: Long, path: String) : Long
    private external fun setTempo(handle: Long, tempo: Int)
    private external fun setVolume(handle: Long, volume: Double)

    // Destructor
    override fun close() = destroyMetronome(handle)
}
