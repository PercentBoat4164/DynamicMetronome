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
    private val secondaryTempoColor = theme.getColor(R.styleable.ProgramDisplay_secondaryTempoColor, Color.LTGRAY)
    private val tempoColor = theme.getColor(R.styleable.ProgramDisplay_tempoColor, Color.DKGRAY)

    private var beatPaint = Paint()
    private var barPaint = Paint()
    private var axisPaint = Paint()
    private var programPaint = Paint()
    private var playHeadPaint = Paint()
    private var secondaryTempoPaint = Paint()
    private var tempoPaint = Paint()
    private var pointRadius = 20f

    private var bars: Array<Long> = arrayOf()
    private var instructions: Map<Long, Instruction> = mapOf()
    private val points = ArrayList<Float>()
    private val barPoints = ArrayList<Float>()
    private var playHead = 0
    private var textRectangle = Rect()

    private var viewSpaceRect = Rect(0, 0, 100, 100)
    private var programSpaceRect = Rect(0, 200, 4, 0)

    init {
        beatPaint.color = beatColor
        beatPaint.strokeWidth = 2f
        beatPaint.textSize = 30f
        barPaint.color = barColor
        barPaint.strokeWidth = 2.5f
        barPaint.textSize = 40f
        axisPaint.color = axisColor
        axisPaint.strokeWidth = 10f
        programPaint.color = programColor
        programPaint.strokeWidth = 5f
        programPaint.isAntiAlias = true
        playHeadPaint.color = playHeadColor
        playHeadPaint.strokeWidth = 5f
        tempoPaint.color = tempoColor
        tempoPaint.strokeWidth = 2f
        tempoPaint.textSize = beatPaint.textSize
        secondaryTempoPaint.color = secondaryTempoColor
        secondaryTempoPaint.strokeWidth = 2.5f
        secondaryTempoPaint.textSize = barPaint.textSize
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        autoDetectProgramSpace()

        // Get the max size of the horizontal text
        initializeViewSpace()
        val oldBottom = viewSpaceRect.bottom
        var maxTextHeight = 0  // Height of text in view space
        for (bar in programSpaceRect.left until  programSpaceRect.right) {
            val string = (bar+1).toString()
            barPaint.getTextBounds(string, 0, string.length, textRectangle)
            viewSpaceRect.bottom = kotlin.math.min(viewSpaceRect.bottom, (oldBottom - textRectangle.width() - axisPaint.strokeWidth).toInt())
            maxTextHeight = kotlin.math.max(maxTextHeight, textRectangle.width())
        }

        // Get the max size of the vertical text
        var maxTextWidth = 0  // Height of text in view space
        for (tempo in programSpaceRect.bottom until  programSpaceRect.top) {
            if (tempo % 10 == 0) {
                val string = tempo.toString()
                secondaryTempoPaint.getTextBounds(string, 0, string.length, textRectangle)
                viewSpaceRect.left = kotlin.math.max(viewSpaceRect.left, (textRectangle.width() + axisPaint.strokeWidth).toInt())
                maxTextWidth = kotlin.math.max(maxTextWidth, textRectangle.width())
            }
        }

        // Only use programSpaceToViewSpace() after the the view space has been determined.
        val beatWidth = programSpaceToViewSpace(1/4.0, 0, false)!!.first - viewSpaceRect.left  // width of beat in view space
        val tempoHeight = programSpaceToViewSpace(0, 1, false)!!.second - viewSpaceRect.bottom // height of tempo in view space

        // Draw the horizontal legend
        canvas.rotate(90F)  // After rotating all points must be (y, -x) to become (x, y)
        for (bar in programSpaceRect.left until programSpaceRect.right) {
            var location = programSpaceToViewSpace(bar, 0, false)!!.first
            barPoints.add(viewSpaceRect.top.toFloat())
            barPoints.add(-location)
            barPoints.add(viewSpaceRect.bottom.toFloat())
            barPoints.add(-location)
            var string = (bar+1).toString()
            barPaint.getTextBounds(string, 0, string.length, textRectangle)
            canvas.drawText(string, viewSpaceRect.bottom.toFloat() + axisPaint.strokeWidth / 2, -location + textRectangle.height() / 2, barPaint)
            if (maxTextHeight > beatWidth) continue
            for (beat in 1..3) {
                location = programSpaceToViewSpace(beat / 4.0 + bar, 0, false)!!.first
                points.add(viewSpaceRect.top.toFloat())
                points.add(-location)
                points.add(viewSpaceRect.bottom.toFloat())
                points.add(-location)
                string = (beat+1).toString()
                beatPaint.getTextBounds(string, 0, string.length, textRectangle)
                canvas.drawText(string, viewSpaceRect.bottom.toFloat() + axisPaint.strokeWidth / 2, -location + textRectangle.height() / 2, beatPaint)
            }
        }
        canvas.drawLines(points.toFloatArray(), beatPaint)
        canvas.drawLines(barPoints.toFloatArray(), barPaint)
        points.clear()
        barPoints.clear()
        canvas.rotate(-90F)

        // Draw the vertical legend
        for (tempo in programSpaceRect.bottom until programSpaceRect.top) {
            val location = programSpaceToViewSpace(0, tempo, false)!!.second
            val string = tempo.toString()
            if (tempo % 10 == 0) {
                barPoints.add(viewSpaceRect.left.toFloat())
                barPoints.add(location)
                barPoints.add(viewSpaceRect.right.toFloat())
                barPoints.add(location)
                secondaryTempoPaint.getTextBounds(string, 0, string.length, textRectangle)
                canvas.drawText(string, viewSpaceRect.left - axisPaint.strokeWidth - textRectangle.width(), location + textRectangle.height() / 2, secondaryTempoPaint)
            } else if (maxTextWidth <= tempoHeight) {
                points.add(viewSpaceRect.left.toFloat())
                points.add(location)
                points.add(viewSpaceRect.right.toFloat())
                points.add(location)
                tempoPaint.getTextBounds(string, 0, string.length, textRectangle)
                canvas.drawText(string, viewSpaceRect.left - axisPaint.strokeWidth - textRectangle.width(), location + textRectangle.height() / 2, tempoPaint)
            }
        }
        canvas.drawLines(points.toFloatArray(), tempoPaint)
        canvas.drawLines(barPoints.toFloatArray(), secondaryTempoPaint)

        points.clear()
        barPoints.clear()

        // Draw the axes
        canvas.drawLines(floatArrayOf(viewSpaceRect.left.toFloat(), viewSpaceRect.top.toFloat(), viewSpaceRect.left.toFloat(), viewSpaceRect.bottom.toFloat(), viewSpaceRect.left.toFloat(), viewSpaceRect.bottom.toFloat(), viewSpaceRect.right.toFloat(), viewSpaceRect.bottom.toFloat()), axisPaint)
        canvas.drawCircle(viewSpaceRect.left.toFloat(), viewSpaceRect.bottom.toFloat(), axisPaint.strokeWidth / 2.0F, axisPaint)

        // Draw the program
        if (instructions.isEmpty()) return
        for (instruction in instructions.entries) {
            // For each set of two elements added to the points, consider that one y, x pair.
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
        canvas.drawLines(points.toFloatArray(), programPaint)
        points.clear()
    }

    private fun initializeViewSpace() {
        viewSpaceRect.left = 0
        viewSpaceRect.right = width
        viewSpaceRect.top = 0
        viewSpaceRect.bottom = height
    }

    private fun autoDetectProgramSpace() {
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

    private fun programSpaceToViewSpace(bar: Number, tempo: Number, bounds: Boolean = true): Pair<Float, Float>? {
        val barF = bar.toFloat()
        val tempoF = tempo.toFloat()
        if (bounds && (barF < programSpaceRect.left || barF > programSpaceRect.right || tempoF < programSpaceRect.bottom || tempoF > programSpaceRect.top)) return null
        // Location in view space = (bar-ps.l)/ps.w*vs.w+vs.l, (tempo-ps.b)/ps.h*vs.h+vs.b
        return Pair((barF - programSpaceRect.left) / programSpaceRect.width() * viewSpaceRect.width() + viewSpaceRect.left, (tempoF - programSpaceRect.bottom) / programSpaceRect.height() * viewSpaceRect.height() + viewSpaceRect.bottom)
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