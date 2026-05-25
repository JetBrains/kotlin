// LANGUAGE: +ContextParameters
import androidx.compose.runtime.Composable


context(foo: Foo)
@Composable
fun A() { }

class Foo { }


fun used(x: Any?) {}
