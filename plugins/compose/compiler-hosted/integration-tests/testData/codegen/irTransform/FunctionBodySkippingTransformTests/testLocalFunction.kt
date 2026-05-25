import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun A(x: Int) {
    @Composable fun foo(y: Int) {
        B(x, y)
    }
    foo(x)
}
