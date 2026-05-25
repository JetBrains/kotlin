import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int) {
    // Composable calls in the result blocks, so we can determine static number of
    // groups executed. This means we put a group around the "then" and the
    // "else" blocks
    if (x > 0) {
        A(a)
    } else {
        A(b)
    }
}
