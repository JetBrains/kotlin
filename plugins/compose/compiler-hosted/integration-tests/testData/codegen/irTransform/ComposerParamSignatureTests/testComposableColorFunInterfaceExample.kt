import androidx.compose.ui.graphics.Color
import java.lang.UnsupportedOperationException

@Composable
public fun Text(text: String, color: Color = Color.Unspecified) {}

@Immutable
@kotlin.jvm.JvmInline
value class Color(val value: ULong) {
    companion object {
        @Stable
        val Red = Color(0xFFFF0000)

        @Stable
        val Blue = Color(0xFF0000FF)
    }
}

@Composable fun condition(): Boolean = true

fun interface ButtonColors {
    @Composable fun getColor(): Color
}
@Composable
fun Button(colors: ButtonColors) {
    Text("hello world", color = colors.getColor())
}
@Composable
fun Test() {
    Button {
        if (condition()) Color.Red else Color.Blue
    }
}

fun used(x: Any?) {}
