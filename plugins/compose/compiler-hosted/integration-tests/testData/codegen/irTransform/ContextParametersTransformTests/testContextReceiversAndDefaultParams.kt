// LANGUAGE: +ContextParameters
import androidx.compose.runtime.Composable


context(_: Foo, _: Bar)
@Composable
fun A(a: Int = 1) { }

context(_: Foo, _: Bar, _: FooBar)
@Composable
fun B(a: Int, b: String = "", c: Int = 1) { }

context(_: Foo)
@Composable
fun C(a: Int, bar: Bar = Bar()) { }
