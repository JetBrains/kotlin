import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


fun Example(a: A) {
    used(a)
    Example { it -> a.compute(it) }
}
