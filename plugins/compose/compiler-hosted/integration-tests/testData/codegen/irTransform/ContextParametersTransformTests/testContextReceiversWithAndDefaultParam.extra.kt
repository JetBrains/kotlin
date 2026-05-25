// LANGUAGE: +ContextParameters
import androidx.compose.runtime.Composable


context(_: Foo)
@Composable
fun String.A(param1: Int, param2: String = "") { }

class Foo { }


fun used(x: Any?) {}
