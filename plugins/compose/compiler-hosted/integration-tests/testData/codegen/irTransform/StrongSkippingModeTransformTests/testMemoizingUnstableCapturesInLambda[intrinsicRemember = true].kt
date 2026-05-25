import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun Test() {
    val foo = Foo(0)
    val lambda = { foo }
}
