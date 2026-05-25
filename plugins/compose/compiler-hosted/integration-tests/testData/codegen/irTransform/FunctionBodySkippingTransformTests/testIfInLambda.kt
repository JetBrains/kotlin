import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun Test(x: Int = 0, y: Int = 0) {
    used(y)
    Wrap {
        if (x > 0) {
            A(x)
        } else {
            A(x)
        }
    }
}
