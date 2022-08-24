package dynamicmetronome.metronome

import java.io.Closeable

/**
 * The Program class
 *
 * Contains a list of instructions which can be parsed by a Metronome to simulate accelerandos, or hold the tempo steady. Instructions can be added, changed, or removed. Programs must be compile before they can be executed by a Metronome.
 */
class Program : Closeable {
    private var handle: Long
    init {
        handle = createProgram()
    }

    constructor(t_handle: Long) {
        handle = t_handle
    }

    constructor(path: String) {
        handle = deserialize(path)
    }

    private external fun createProgram() : Long
    private external fun destroyProgram(handle: Long)

    fun addOrChangeInstruction(bar: Long, tempo: Int, interpolate: Boolean) = addOrChangeInstruction(handle, bar, tempo, interpolate)
    fun compile() = compile(handle)
    fun length() = length(handle)
    fun clear() = clear(handle)
    fun getName() = getName(handle)
    fun serialize() = serialize(handle)

    private external fun addOrChangeInstruction(handle: Long, bar: Long, tempo: Int, interpolate: Boolean)
    private external fun compile(handle: Long) : DoubleArray
    private external fun length(handle: Long) : Long
    private external fun clear(handle: Long)
    private external fun getName(handle: Long): String
    private external fun serialize(handle: Long): String
    private external fun deserialize(path: String): Long

    // Destructor
    override fun close() = destroyProgram(handle)
}