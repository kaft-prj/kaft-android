package org.kaft.kaft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.kaft.kaft.ui.theme.KaftTheme
import org.kaft.kaft.ui.theme.PinkClockBackground
import org.kaft.kaft.ui.theme.PinkClockRemain
import java.util.logging.Logger
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val logger = Logger.getLogger("MainActivity")

data class ClockState(
    val isRunning: Boolean = false,
    val overallTimeMillis: Long = 36000,
    val remainTimeMillis: Long = 30000,
    val currentTimestamp: Long = System.currentTimeMillis(),
    val destTimestamp: Long = System.currentTimeMillis(),
) {

    private fun start() = this.copy(
        isRunning = true,
        destTimestamp = currentTimestamp + remainTimeMillis,
    )

    private fun stop() = this.copy(
        isRunning = false,
        remainTimeMillis = destTimestamp - currentTimestamp,
    )

    fun setRunning(running: Boolean) = if (running) start() else stop()

    fun setDegree(deg: Float) = if (this.isRunning) this else this.copy(
        remainTimeMillis = (overallTimeMillis.toFloat() * deg / 360.0f).toLong(),
    )

    fun update() = this.copy(
        currentTimestamp = System.currentTimeMillis(),
    )

    val sweepAngle: Float
        get() {
            val result = (if (isRunning)
                destTimestamp - currentTimestamp
            else
                remainTimeMillis) / overallTimeMillis.toFloat() * 360.0f
            return result
        }

}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            KaftTheme {
                KaftMainContent()
            }
        }
    }

    @Composable
    private fun KaftMainContent() {
        var uiState by remember { mutableStateOf(ClockState()) }
        val colors = MaterialTheme.colorScheme

        LaunchedEffect(Unit) {
            while (true) {
                delay(100)
                if (uiState.isRunning) {
                    uiState = uiState.update()
                }
            }
        }

        Column(
            modifier = Modifier
                .background(colors.background)
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            TitleRow()
            ClockRow(
                sweepAngle = uiState.sweepAngle,
                onClick = {
                    uiState = uiState.setDegree(it)
                },
            )
            ButtonRow(
                isRunning = uiState.isRunning,
                setRunning = {
                    uiState = uiState.setRunning(it)
                },
            )
        }
    }

    @Composable
    private fun ButtonRow(
        isRunning: Boolean,
        setRunning: (Boolean) -> Unit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Button(
                modifier = Modifier.padding(vertical = 40.dp),
                onClick = {
                    setRunning(!isRunning)
                }
            ) {
                Text(text = if (isRunning) "STOP" else "START")
            }
        }
    }

    @Composable
    private fun ClockRow(
        sweepAngle: Float,
        onClick: (Float) -> Unit,
    ) {
        val clockSize = 200.dp
        val halfClockSize = clockSize.div(2)
        val graduationSize = 30.dp
        val graduationOffset = 15.dp
        val graduationOffset2 = 23.dp
        val backgroundSize = clockSize + graduationSize * 2

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .padding(0.dp)
                    .width(backgroundSize)
                    .height(backgroundSize)
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val clockCenter = Offset(
                                backgroundSize
                                    .div(2)
                                    .toPx(),
                                backgroundSize
                                    .div(2)
                                    .toPx(),
                            )
                            val directionOffset = tapOffset - clockCenter
                            val radian = (atan2(directionOffset.y, directionOffset.x) + Math.PI / 2)
                                .let { if (it < 0) it + Math.PI * 2 else it }
                            val degree = radian * 180 / Math.PI
                            onClick(degree.toFloat())
                        }
                    }
            ) {
                val clockOffset = Offset(
                    graduationSize.toPx(),
                    graduationSize.toPx(),
                )
                val clockCenter = Offset(
                    backgroundSize.div(2).toPx(),
                    backgroundSize.div(2).toPx(),
                )

                drawArc(
                    color = PinkClockBackground,
                    startAngle = 0.0f,
                    sweepAngle = 360.0f,
                    useCenter = true,
                    topLeft = clockOffset,
                    size = Size(clockSize.toPx(), clockSize.toPx()),
                    style = Fill,
                )

                drawArc(
                    color = PinkClockRemain,
                    startAngle = -90.0f,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = clockOffset,
                    size = Size(clockSize.toPx(), clockSize.toPx()),
                    style = Fill,
                )

                for (deg in 0..<360 step 6) {
                    val isBig = (deg % 30) == 0
                    val graduationOffset0 = if (isBig) graduationOffset else graduationOffset2
                    val fDeg = deg.toFloat() * Math.PI.toFloat() / 180.0f
                    val direction = Offset(cos(fDeg), sin(fDeg))
                    drawLine(
                        color = Color.Black,
                        start = clockCenter + direction * (halfClockSize + graduationOffset0).toPx(),
                        end = clockCenter + direction * (halfClockSize + graduationSize).toPx(),
                    )
                }
            }
        }
    }

    @Composable
    private fun TitleRow() {
        Row(
            modifier = Modifier
                .padding(vertical = 60.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "무엇에 집중하실건가요?",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(100.dp))
        }
    }

}
