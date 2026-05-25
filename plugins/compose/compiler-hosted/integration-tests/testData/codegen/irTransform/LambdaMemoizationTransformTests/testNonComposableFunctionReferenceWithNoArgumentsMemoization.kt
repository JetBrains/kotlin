import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class Stable { fun qux() {} }

@Composable
fun Something() {
    val x = remember { Stable() }
    val shouldMemoize = x::qux
}
