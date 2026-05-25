import androidx.compose.runtime.Composable


class Unstable(var foo: Int = 0)
fun Unstable.method(arg1: Int) {}
val unstable = Unstable()

fun used(x: Any?) {}
