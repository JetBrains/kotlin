import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@ReadOnlyComposable
@Composable
fun getSomeValue(a: Int): Int {
    if (a < 100) return 0
    return 1
}
