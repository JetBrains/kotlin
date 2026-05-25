// LANGUAGE: +ContextParameters
import androidx.compose.runtime.Composable


context(foo: Foo, bar: Bar)
@Composable
fun A() { }

context(foo: Foo, bar: Bar, fooBar: FooBar)
@Composable
fun B() { }
