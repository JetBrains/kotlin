import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember

class Stable
fun Stable.foo() {}

@NonRestartableComposable
@Composable
fun Example() {
    val x = remember { Stable() }
    val shouldMemoize = x::foo
}
