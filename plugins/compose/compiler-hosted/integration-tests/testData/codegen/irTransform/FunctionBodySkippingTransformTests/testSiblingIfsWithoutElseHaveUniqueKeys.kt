import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun Test(cond: Boolean) {
    if (cond) {
        A()
    }
    if (cond) {
        B()
    }
}
