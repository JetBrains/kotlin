import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class Stable { fun qux(arg1: Any) {} }

@Composable
fun Something() {
    val x = remember { Stable() }
    val shouldMemoize = x::qux
}
