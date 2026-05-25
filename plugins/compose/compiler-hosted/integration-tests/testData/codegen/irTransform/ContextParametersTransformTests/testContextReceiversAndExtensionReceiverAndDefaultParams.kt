// LANGUAGE: +ContextParameters
import androidx.compose.runtime.Composable


context(foo: Foo, bar: Bar, fooBar: FooBar)
@Composable
fun String.B(a: Int, b: String = "", c: Int = 1) { }
