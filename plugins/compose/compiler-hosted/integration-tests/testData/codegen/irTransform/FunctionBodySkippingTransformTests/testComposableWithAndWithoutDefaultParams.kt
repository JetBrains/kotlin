import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable fun Wrap(y: Int, content: @Composable (x: Int) -> Unit) {
    content(y)
}
@Composable
fun Test(x: Int = 0, y: Int = 0) {
    used(y)
    Wrap(10) {
        used(it)
        A(x)
    }
}
