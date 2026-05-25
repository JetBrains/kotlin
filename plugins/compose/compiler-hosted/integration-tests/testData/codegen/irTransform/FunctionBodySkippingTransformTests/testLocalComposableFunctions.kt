import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun Example(a: Int) {
    @Composable fun Inner() {
        A(a)
    }
    Inner()
}
