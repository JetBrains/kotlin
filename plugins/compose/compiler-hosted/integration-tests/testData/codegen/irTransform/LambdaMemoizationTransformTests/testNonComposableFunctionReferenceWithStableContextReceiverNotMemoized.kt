// IGNORE_BACKEND_K2: JVM_IR
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class StableReceiver
class Stable {
    context(_: StableReceiver)
    fun qux() {}
}

@Composable
fun Something() {
    val x = remember { Stable() }
    val shouldNotMemoize = x::qux
}

fun used(x: Any?) {}
