import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


class Foo(val value: Int = 0)

@Composable
fun Test(x: Foo) {
    used(x)
}
