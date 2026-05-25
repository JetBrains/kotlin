import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int) {
    // Since the first condition of an if/else is unconditionally executed, it does not
    // necessitate a group of any kind, so we just end up with the function boundary
    // group
    if (B()) {
        NA()
    } else {
        NA()
    }
}
