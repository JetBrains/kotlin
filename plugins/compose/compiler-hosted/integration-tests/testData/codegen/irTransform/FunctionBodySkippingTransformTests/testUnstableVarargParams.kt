import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


class Foo(var value: Int = 0)

@Composable
fun B(vararg values: Foo) {
    print(values)
}
