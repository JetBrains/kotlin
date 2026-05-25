import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun Example(a: Int = 0, b: Int = makeInt(), c: Int = 0) {
    used(a)
    used(b)
    used(c)
}
