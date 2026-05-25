import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int) {
    // Only one composable call in the result blocks, so we can just generate
    // a single group around the whole expression.
    if (x > 0) {
        A()
    }
}
