import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


class Foo(var value: Int = 0)
class Bar(val value: Int = 0)

@Composable
fun Test(foo: Foo, bar: Bar) {
    val lambda: @Composable ()->Unit = {
        foo
        bar
    }
}
