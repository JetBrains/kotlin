import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int) {
    // nothing needed except for the function boundary group
    when (x) {
        0 -> 8
        1 -> 10
        else -> x
    }
}
