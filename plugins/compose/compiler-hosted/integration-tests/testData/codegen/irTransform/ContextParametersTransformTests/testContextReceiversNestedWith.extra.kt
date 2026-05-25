// LANGUAGE: +ContextParameters
import androidx.compose.runtime.Composable


context(foo: Foo)
@Composable
fun A() { }

context(foo: Foo, bar: Bar)
@Composable
fun B() { }

class Foo { }
class Bar { }


fun used(x: Any?) {}
