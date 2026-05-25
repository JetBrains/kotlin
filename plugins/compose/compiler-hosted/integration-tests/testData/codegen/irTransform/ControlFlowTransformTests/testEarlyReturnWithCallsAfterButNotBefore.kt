import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int) {
    // we can just close out the open groups at the return.
    if (x > 0) {
        return
    }
    A()
}
