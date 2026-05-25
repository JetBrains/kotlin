import androidx.compose.runtime.Composable


@Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
class Foo(var value: Int = 0)
fun Lam(x: ()->Unit) { x() }

fun used(x: Any?) {}
