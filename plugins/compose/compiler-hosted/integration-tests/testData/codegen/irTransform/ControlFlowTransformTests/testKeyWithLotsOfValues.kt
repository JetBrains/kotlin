import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(a: Int, b: Int, c: Int, d: Int) {
    key(a, b, c, d) {
        A()
    }
}
