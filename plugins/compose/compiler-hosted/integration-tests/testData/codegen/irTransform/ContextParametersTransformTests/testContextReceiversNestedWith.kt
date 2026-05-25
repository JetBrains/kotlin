// LANGUAGE: +ContextParameters
import androidx.compose.runtime.Composable


@Composable
fun Test(foo: Foo) {
    with(foo) {
        A()
        with(Bar()) {
            B()
        }
    }
}
