import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int): Int {
    return if (x > 0) {
        if (B()) 1
        else if (B()) 2
        else 3
    } else 4
}
