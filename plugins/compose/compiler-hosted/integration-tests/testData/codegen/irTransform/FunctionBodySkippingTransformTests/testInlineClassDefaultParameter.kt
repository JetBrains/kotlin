import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


inline class Color(val value: Int) {
    companion object {
        val Unset = Color(0)
    }
}

@Composable
fun A(text: String) {
    B(text)
}

@Composable
fun B(text: String, color: Color = Color.Unset) {
    used(text)
    used(color)
}
