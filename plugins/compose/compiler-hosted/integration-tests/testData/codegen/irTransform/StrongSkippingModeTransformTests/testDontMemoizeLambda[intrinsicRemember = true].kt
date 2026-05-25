import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


import androidx.compose.runtime.DontMemoize

@Composable
fun Test() {
    val foo = Foo(0)
    val lambda = @DontMemoize { foo }
    Lam @DontMemoize { foo }
}
