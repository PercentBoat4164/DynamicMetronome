package dynamicmetronome.gui.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import dynamicmetronome.activities.R
import dynamicmetronome.metronome.Program
import kotlin.math.max
import kotlin.math.min

class ProgramDisplay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val theme = context.theme.obtainStyledAttributes(attrs, R.styleable.ProgramDisplay, 0, 0)
    private val backgroundColor = theme.getColor(R.styleable.ProgramDisplay_backgroundColor, Color.BLACK)
    private val axisColor = theme.getColor(R.styleable.ProgramDisplay_axisColor, Color.WHITE)
    private val barColor = theme.getColor(R.styleable.ProgramDisplay_barColor, Color.LTGRAY)
    private val beatColor = theme.getColor(R.styleable.ProgramDisplay_beatColor, Color.DKGRAY)
    private val programColor = theme.getColor(R.styleable.ProgramDisplay_programColor, Color.BLUE)
    private val playHeadColor = theme.getColor(R.styleable.ProgramDisplay_playHeadColor, Color.GREEN)
    private val tempoColor = theme.getColor(R.styleable.ProgramDisplay_tempoColor, Color.DKGRAY)
    private val legendColor = theme.getColor(R.styleable.ProgramDisplay_legendColor, Color.WHITE)

    private val maxVerticalTextBounds = Rect()
    private val maxHorizontalTextBounds = Rect()

    private var horizontalPadding = 100f
    private var verticalPadding = 100f

    private var beatPaint = Paint()

    private var barPaint = Paint()
    private var axisPaint = Paint()
    private var programPaint = Paint()
    private var playHeadPaint = Paint()
    private var tempoPaint = Paint()
    private var legendPaint = Paint()
    private var pointRadius = 20f

    private var bars: Array<Long> = arrayOf()
    private var program: Array<Pair<Float, Float>> = arrayOf()
    private var playHead = 0

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

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var graphHorizontalScale = 0f
        for (beats in bars) graphHorizontalScale += beats
        var graphVerticalScaleTop = 0f
        var graphVerticalScaleBottom = Float.MAX_VALUE
        for (beat in program) {
            graphVerticalScaleTop = max(graphVerticalScaleTop, beat.second)
            graphVerticalScaleBottom = min(graphVerticalScaleBottom, beat.second)
        }

        legendPaint.getTextBounds(graphVerticalScaleTop.toString(), 0, graphVerticalScaleTop.toString().length, maxVerticalTextBounds)
        horizontalPadding = maxVerticalTextBounds.width().toFloat()

        // Calculate max beat text height
        var maxTextWidth = 0f
        for (i in bars.indices) {
            for (beat in 1..bars[i]) {
                legendPaint.getTextBounds("$i:$beat", 0, "$i:$beat".length, maxHorizontalTextBounds)
                maxTextWidth = max(maxHorizontalTextBounds.width().toFloat(), maxTextWidth)
            }
        }
        legendPaint.getTextBounds("${bars.size}:${1}", 0, "${bars.size}:${1}".length, maxHorizontalTextBounds)
        maxTextWidth = max(maxHorizontalTextBounds.width().toFloat(), maxTextWidth)
        maxHorizontalTextBounds.set(maxHorizontalTextBounds.left, maxHorizontalTextBounds.top, maxTextWidth.toInt(), maxHorizontalTextBounds.bottom)
        verticalPadding = maxTextWidth + pointRadius

        val graphLeft = horizontalPadding
        val graphRight = width.toFloat()
        val graphTop = 0f
        val graphBottom = height.toFloat() - verticalPadding
        val beatWidth = (graphRight - graphLeft) / graphHorizontalScale

        val drawProgram = ArrayList<Pair<Float, Float>>(program.size)
        for (beat in program) drawProgram.add(Pair(graphLeft + beat.first * beatWidth,
            (beat.second - graphVerticalScaleBottom) / (graphVerticalScaleTop - graphVerticalScaleBottom) * (graphTop - graphBottom) + graphBottom))

        var position: Float

        // Draw graph background
        canvas.drawColor(backgroundColor)

        // Draw vertical legend and lines
        for (i in graphVerticalScaleBottom.toInt() until graphVerticalScaleTop.toInt() step 10) {
            position = (i - graphVerticalScaleBottom) / (graphVerticalScaleTop - graphVerticalScaleBottom) * (graphTop - graphBottom) + graphBottom
            canvas.drawText(i.toString(), 0f,  position - maxVerticalTextBounds.exactCenterY(), legendPaint)
            canvas.drawLine(graphLeft, position, graphRight, position, tempoPaint)
        }
        canvas.drawText(graphVerticalScaleTop.toInt().toString(), 0f,  0f + maxVerticalTextBounds.height(), legendPaint)
        canvas.drawLine(graphLeft, 0f, graphRight, 0f, tempoPaint)

        position = graphLeft

        // Draw horizontal lines
        for (i in bars.indices) {
            canvas.drawLine(position, graphBottom, position, graphTop, barPaint)
            for (beat in 1..bars[i]) {
                position += beatWidth
                canvas.drawLine(position, graphBottom, position, graphTop, beatPaint)
            }
        }

        // Draw horizontal legend
        position = graphLeft
        canvas.rotate(90f)
        for (bar in bars.indices) {
            for (beat in 1..bars[bar]) {
                canvas.drawText("${bar+1}:$beat", graphBottom + pointRadius, -position - maxHorizontalTextBounds.exactCenterY(), legendPaint)
                position += beatWidth
            }
        }
        canvas.drawText("${bars.size+1}:1", graphBottom + pointRadius, -position + maxHorizontalTextBounds.height(), legendPaint)
        canvas.rotate(-90f)

        // Draw axes
        canvas.drawLine(graphLeft, graphBottom - axisPaint.strokeWidth / 2, graphRight, graphBottom - axisPaint.strokeWidth / 2, axisPaint)
        canvas.drawLine(graphLeft + axisPaint.strokeWidth / 2, graphBottom, graphLeft + axisPaint.strokeWidth / 2, graphTop, axisPaint)

        // Draw play head
        canvas.drawLine(axisPaint.strokeWidth + graphLeft + playHead * beatWidth, graphBottom, axisPaint.strokeWidth + graphLeft + playHead * beatWidth, graphTop, playHeadPaint)

        // Draw program
        for (beat in 0..program.size - 2) {
            canvas.drawLine(
                drawProgram[beat].first,
                drawProgram[beat].second,
                drawProgram[beat + 1].first,
                drawProgram[beat + 1].second,
                programPaint
            )
            canvas.drawCircle(
                drawProgram[beat].first,
                drawProgram[beat].second,
                pointRadius,
                programPaint)
        }
        if (program.isNotEmpty())
            canvas.drawCircle(
                drawProgram.last().first,
                drawProgram.last().second,
                pointRadius,
                programPaint)
    }

    fun setProgram(inputProgram: Program) {
        val instructions = inputProgram.getInstructionsAndBars()
        val programData = ArrayList<Pair<Float, Float>>(inputProgram.length())
        val barsData = ArrayList<Long>(inputProgram.length())
        var cumulativeBeats = 0f
        for (i in instructions.indices) {
            for (j in 0 until instructions[i].first) barsData.add(4)
            cumulativeBeats += instructions[i].first.toFloat() * 4
            programData.add(Pair(cumulativeBeats, instructions[i].second.startTempo.toFloat()))
            try {  // This exception will always be triggered on the last run of this loop.
                // If there is not interpolation and there is a change in tempo.
                if (instructions[i].second.startTempo != instructions[i + 1].second.startTempo &&
                    instructions[i].second.tempoOffset == 0.0)
                    programData.add(Pair(cumulativeBeats, instructions[i].second.startTempo.toFloat()))
            } catch (_: IndexOutOfBoundsException) {}
        }
        bars = barsData.toTypedArray()
        program = programData.toTypedArray()
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

    fun draw() {
        invalidate()
    }
}