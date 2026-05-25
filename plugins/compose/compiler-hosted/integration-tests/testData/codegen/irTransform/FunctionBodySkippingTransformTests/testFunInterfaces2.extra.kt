import androidx.compose.runtime.Composable


import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
fun Color(color: Long): Color {
    return Color((color shl 32).toULong())
}

@Immutable
@kotlin.jvm.JvmInline
value class Color(val value: ULong) {
    companion object {
        @Stable
        val Red = Color(0xFFFF0000)
        @Stable
        val Blue = Color(0xFF0000FF)
        @Stable
        val Transparent = Color(0x00000000)
    }
}

@Composable
public fun Text(
    text: String,
    color: Color = Color.Transparent,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {}

@Composable fun condition(): Boolean = true

fun interface ButtonColors {
    @Composable fun getColor(): Color
}

fun used(x: Any?) {}
