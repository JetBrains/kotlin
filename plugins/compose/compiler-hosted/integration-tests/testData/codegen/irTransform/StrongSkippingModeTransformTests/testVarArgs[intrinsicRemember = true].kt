import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable fun Varargs(vararg ints: Int) {
}
@Composable fun Test() {
    Varargs(1, 2, 3)
}
