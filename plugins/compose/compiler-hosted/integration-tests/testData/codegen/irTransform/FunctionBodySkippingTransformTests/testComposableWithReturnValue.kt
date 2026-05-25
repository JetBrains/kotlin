import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun Test(x: Int = 0, y: Int = 0): Int {
    A(x, y)
    return x + y
}
