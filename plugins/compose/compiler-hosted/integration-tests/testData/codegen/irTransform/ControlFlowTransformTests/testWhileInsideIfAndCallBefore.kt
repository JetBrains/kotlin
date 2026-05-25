import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int) {
    if (x > 0) {
        A()
        while (x > 0) {
            A()
        }
    }
}
