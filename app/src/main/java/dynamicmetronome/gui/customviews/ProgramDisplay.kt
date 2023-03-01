package dynamicmetronome.gui.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import dynamicmetronome.activities.R
import dynamicmetronome.metronome.Instruction
import dynamicmetronome.metronome.Program

class ProgramDisplay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val theme = context.theme.obtainStyledAttributes(attrs, R.styleable.ProgramDisplay, 0, 0)
    private val axisColor = theme.getColor(R.styleable.ProgramDisplay_axisColor, Color.WHITE)
    private val barColor = theme.getColor(R.styleable.ProgramDisplay_barColor, Color.LTGRAY)
    private val beatColor = theme.getColor(R.styleable.ProgramDisplay_beatColor, Color.DKGRAY)
    private val programColor = theme.getColor(R.styleable.ProgramDisplay_programColor, Color.BLUE)
    private val playHeadColor = theme.getColor(R.styleable.ProgramDisplay_playHeadColor, Color.GREEN)
    private val tempoColor = theme.getColor(R.styleable.ProgramDisplay_tempoColor, Color.DKGRAY)
    private val legendColor = theme.getColor(R.styleable.ProgramDisplay_legendColor, Color.WHITE)

    private var beatPaint = Paint()

    private var barPaint = Paint()
    private var axisPaint = Paint()
    private var programPaint = Paint()
    private var playHeadPaint = Paint()
    private var tempoPaint = Paint()
    private var legendPaint = Paint()
    private var pointRadius = 20f

    private var bars: Array<Long> = arrayOf()
    private var instructions: Map<Long, Instruction> = mapOf()
    private val points = ArrayList<Float>()
    private var playHead = 0

    private var viewSpaceRect = Rect(0, 0, 100, 100)
    private var programSpaceRect = Rect(0, 200, 4, 0)

    init {
        beatPaint.color = beatColor
        barPaint.color = barColor
        barPaint.strokeWidth = 2.5f
        axisPaint.color = axisColor
        axisPaint.strokeWidth = 10f
        programPaint.color = programColor
        programPaint.strokeWidth = 5f
        programPaint.isAntiAlias = true
        playHeadPaint.color = playHeadColor
        playHeadPaint.strokeWidth = 5f
        tempoPaint.color = tempoColor
        legendPaint.color = legendColor
        legendPaint.textSize = 40f
        legendPaint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

//        instructions = mapOf(Pair(1, Instruction(120.0, true)), Pair(3, Instruction(140.0, false)))

        getViewSpace()
        getProgramSpace()

        // Draw the axes
        canvas.drawLines(floatArrayOf(viewSpaceRect.left.toFloat(), viewSpaceRect.top.toFloat(), viewSpaceRect.left.toFloat(), viewSpaceRect.bottom.toFloat(), viewSpaceRect.left.toFloat(), viewSpaceRect.bottom.toFloat(), viewSpaceRect.right.toFloat(), viewSpaceRect.bottom.toFloat()), axisPaint)
        canvas.drawCircle(viewSpaceRect.left.toFloat(), viewSpaceRect.bottom.toFloat(), axisPaint.strokeWidth / 2.0F, axisPaint)

        // Draw the program
        if (instructions.isEmpty()) return

        points.clear()

        for (instruction in instructions.entries) {
            // For each set of two elements added to the points, consider that one x, y pair.
            // For each set of two points added to the points, consider that the end of one line and
            //     the  start of another. The first and last points are removed at the end to make
            //     this work.
            val location = programSpaceToViewSpace(instruction.key, instruction.value.tempo) ?: continue
            val y = points.lastOrNull()
            if (y != null && !instruction.value.interpolation && y != location.second) {
                // No interpolation and the tempo changes.
                points.add(location.first)
                points.add(y)
                points.add(location.first)
                points.add(y)
                // Fancy corners
                canvas.drawCircle(location.first, y, programPaint.strokeWidth / 2.0F, programPaint)
            }
            points.add(location.first)
            points.add(location.second)
            points.add(location.first)
            points.add(location.second)
            canvas.drawCircle(location.first, location.second, pointRadius, programPaint)
        }
        points.removeLast()
        points.removeLast()
        points.removeFirst()
        points.removeFirst()
        canvas.drawLines(points.toTypedArray().toFloatArray(), programPaint)
    }

    private fun getViewSpace() {
        viewSpaceRect.left = (width * .1).toInt()
        viewSpaceRect.right = width
        viewSpaceRect.top = 0
        viewSpaceRect.bottom = height - viewSpaceRect.left
    }

    private fun getProgramSpace() {
        if (instructions.isEmpty()) {
            programSpaceRect.left = 0
            programSpaceRect.right = 1
            programSpaceRect.top = 200
            programSpaceRect.bottom = 0
        } else {
            programSpaceRect.left = 0
            programSpaceRect.right = 0
            programSpaceRect.top = 0
            programSpaceRect.bottom = Int.MAX_VALUE
            for (instruction in instructions.entries) {
                programSpaceRect.top = kotlin.math.max(programSpaceRect.top, kotlin.math.ceil(instruction.value.tempo).toInt())
                programSpaceRect.bottom = kotlin.math.min(programSpaceRect.bottom, kotlin.math.floor(instruction.value.tempo).toInt())
                programSpaceRect.right = kotlin.math.max(programSpaceRect.right, instruction.key.toInt())
            }
            programSpaceRect.top = (programSpaceRect.top * 1.1).toInt()
            programSpaceRect.bottom = (programSpaceRect.bottom * 0.9).toInt()
        }
    }

    private fun programSpaceToViewSpace(bar: Long, tempo: Double): Pair<Float, Float>? {
        // null if outside of the given program space.
        if (bar < programSpaceRect.left || bar > programSpaceRect.right || tempo < programSpaceRect.bottom || tempo > programSpaceRect.top) return null
        // Location in view space = (bar-ps.l)/ps.w*vs.w+vs.l, (tempo-ps.b)/ps.h*vs.h+vs.b
        return Pair((bar - programSpaceRect.left).toFloat() / programSpaceRect.width() * viewSpaceRect.width() + viewSpaceRect.left, (tempo - programSpaceRect.bottom).toFloat() / programSpaceRect.height() * viewSpaceRect.height() + viewSpaceRect.bottom)
    }

    private fun getLegendDrawLevel() {
        // Get all text bounds and locations
        // If any overlap
          // Restart with one text level down
        // Return some identifier for a text level to render
    }

    fun setProgram(inputProgram: Program) {
        instructions = inputProgram.getInstructions()
        bars = instructions.keys.toTypedArray()
        draw()
    }

    fun resetPlayHead() {
        playHead = 0
        draw()
    }

    fun movePlayHead() {
        ++playHead
        draw()
    }

    fun setPlayHead(position: Int) {
        playHead = position
        draw()
    }

    private fun draw() {
        invalidate()
    }
}