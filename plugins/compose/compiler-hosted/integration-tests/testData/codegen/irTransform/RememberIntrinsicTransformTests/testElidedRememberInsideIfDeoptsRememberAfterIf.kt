import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


import androidx.compose.runtime.NonRestartableComposable

@Composable
@NonRestartableComposable
fun app(x: Boolean) {
    val a = if (x) { remember { 1 } } else { 2 }
    val b = remember { 2 }
}
