import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


class Holder<T> {
    @Composable
    fun Test(x: T) {
        A(x as Int)
    }
}
