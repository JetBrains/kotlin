import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


class Foo(var value: Int = 0)
@Composable fun A(x: Int) {}
@Composable fun B(y: Foo) {}

val foo = @Composable { x: Int, y: Foo ->
    A(x)
    B(y)
}
