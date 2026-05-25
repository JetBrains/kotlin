import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


val foo = @Composable { x: Int, y: Foo ->
    A(x)
    B(y)
}
