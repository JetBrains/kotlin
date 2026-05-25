// LANGUAGE: +ContextParameters
import androidx.compose.runtime.Composable


@Composable
fun Test(foo: Foo) {
    with(foo) {
      "Hello".A(2)
    }
}
