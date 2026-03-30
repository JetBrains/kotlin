package demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay
import java.awt.Rectangle
import java.awt.Robot
import java.io.File
import javax.imageio.ImageIO

private val materialStringText:
        @Composable (
            text: String,
            modifier: Modifier,
            color: Color,
            fontSize: TextUnit,
            fontStyle: FontStyle?,
            fontWeight: FontWeight?,
            fontFamily: FontFamily?,
            letterSpacing: TextUnit,
            textDecoration: TextDecoration?,
            textAlign: TextAlign?,
            lineHeight: TextUnit,
            overflow: TextOverflow,
            softWrap: Boolean,
            maxLines: Int,
            minLines: Int,
            onTextLayout: ((TextLayoutResult) -> Unit)?,
            style: TextStyle,
        ) -> Unit = ::Text

@Composable
fun AddText(...Text.$props(materialStringText)) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}

@Composable
fun TitleText(...AddText.$props) {
    val resolvedFontSize = if (fontSize == TextUnit.Unspecified) {
        28.sp
    } else {
        fontSize
    }
    val resolvedWeight = fontWeight ?: FontWeight.Bold

    AddText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = resolvedFontSize,
        fontStyle = fontStyle,
        fontWeight = resolvedWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}

@Composable
fun TraceableText(
    ...Text.$attrs(materialStringText),
    ...Text.$callbacks(materialStringText),
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}

open class TextPropsCarrier(
    val text: String,
    val modifier: Modifier = Modifier,
    val color: Color = Color.Unspecified,
    val fontSize: TextUnit = TextUnit.Unspecified,
    val fontStyle: FontStyle? = null,
    val fontWeight: FontWeight? = null,
    val fontFamily: FontFamily? = null,
    val letterSpacing: TextUnit = TextUnit.Unspecified,
    val textDecoration: TextDecoration? = null,
    val textAlign: TextAlign? = null,
    val lineHeight: TextUnit = TextUnit.Unspecified,
    val overflow: TextOverflow = TextOverflow.Clip,
    val softWrap: Boolean = true,
    val maxLines: Int = Int.MAX_VALUE,
    val minLines: Int = 1,
    val onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    val style: TextStyle = TextStyle.Default,
)

class BoundTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = TextStyle.Default,
) : TextPropsCarrier(
    text = text,
    modifier = modifier,
    color = color,
    fontSize = fontSize,
    fontStyle = fontStyle,
    fontWeight = fontWeight,
    fontFamily = fontFamily,
    letterSpacing = letterSpacing,
    textDecoration = textDecoration,
    textAlign = textAlign,
    lineHeight = lineHeight,
    overflow = overflow,
    softWrap = softWrap,
    maxLines = maxLines,
    minLines = minLines,
    onTextLayout = onTextLayout,
    style = style,
) {
    @Composable
    fun Render() {
        Text(
            ...TextPropsCarrier.$props(this).exclude(fontSize, fontWeight),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DemoScreen() {
    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TitleText(
                    text = "Spread pack Compose demo",
                    modifier = Modifier,
                    color = Color(0xFF1F5EFF),
                    fontSize = TextUnit.Unspecified,
                    fontStyle = null,
                    fontWeight = null,
                    fontFamily = null,
                    letterSpacing = TextUnit.Unspecified,
                    textDecoration = null,
                    textAlign = null,
                    lineHeight = TextUnit.Unspecified,
                    overflow = TextOverflow.Clip,
                    softWrap = true,
                    maxLines = Int.MAX_VALUE,
                    minLines = 1,
                    onTextLayout = null,
                    style = TextStyle.Default,
                )
                AddText(
                    text = "TitleText 通过 ...AddText.\$props 继承 text/color/fontSize/fontWeight/maxLines。",
                    modifier = Modifier,
                    color = Color(0xFF2E3A59),
                    fontSize = 15.sp,
                    fontStyle = null,
                    fontWeight = null,
                    fontFamily = null,
                    letterSpacing = TextUnit.Unspecified,
                    textDecoration = null,
                    textAlign = null,
                    lineHeight = TextUnit.Unspecified,
                    overflow = TextOverflow.Clip,
                    softWrap = true,
                    maxLines = Int.MAX_VALUE,
                    minLines = 1,
                    onTextLayout = null,
                    style = TextStyle.Default,
                )
                TraceableText(
                    text = "TraceableText 通过真实 Text 重载拆分出 \$attrs 和 \$callbacks。",
                    modifier = Modifier,
                    color = Color(0xFF5B647A),
                    fontSize = 14.sp,
                    fontStyle = null,
                    fontWeight = null,
                    fontFamily = null,
                    letterSpacing = TextUnit.Unspecified,
                    textDecoration = null,
                    textAlign = null,
                    lineHeight = TextUnit.Unspecified,
                    overflow = TextOverflow.Clip,
                    softWrap = true,
                    maxLines = Int.MAX_VALUE,
                    minLines = 1,
                    style = TextStyle.Default,
                    onTextLayout = {},
                )
                BoundTitleText(
                    text = "BoundTitleText 通过 T.\$props(this).exclude(fontSize, fontWeight) 复用载体属性。",
                    modifier = Modifier,
                    color = Color(0xFF7A3E00),
                    fontSize = 12.sp,
                    fontStyle = null,
                    fontWeight = FontWeight.Normal,
                    fontFamily = null,
                    letterSpacing = TextUnit.Unspecified,
                    textDecoration = null,
                    textAlign = null,
                    lineHeight = TextUnit.Unspecified,
                    overflow = TextOverflow.Clip,
                    softWrap = true,
                    maxLines = Int.MAX_VALUE,
                    minLines = 1,
                    onTextLayout = null,
                    style = TextStyle.Default,
                ).Render()
            }
        }
    }
}

@Composable
private fun WindowScope.CaptureDemoScreenshotEffect(
    outputPath: String,
    onCaptured: () -> Unit,
) {
    LaunchedEffect(window, outputPath) {
        delay(1000)
        window.toFront()
        window.requestFocus()
        delay(400)

        val bounds = window.bounds
        val screenshot = Robot().createScreenCapture(
            Rectangle(bounds.x, bounds.y, bounds.width, bounds.height)
        )
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        ImageIO.write(screenshot, "png", outputFile)
        println("Saved spread-pack demo screenshot to ${outputFile.absolutePath}")
        onCaptured()
    }
}

fun main() = application {
    val screenshotOutput = System.getProperty("spread.pack.demo.screenshot")
    Window(
        onCloseRequest = ::exitApplication,
        title = "Spread Pack Compose Demo",
    ) {
        if (!screenshotOutput.isNullOrBlank()) {
            CaptureDemoScreenshotEffect(
                outputPath = screenshotOutput,
                onCaptured = ::exitApplication,
            )
        }
        DemoScreen()
    }
}
