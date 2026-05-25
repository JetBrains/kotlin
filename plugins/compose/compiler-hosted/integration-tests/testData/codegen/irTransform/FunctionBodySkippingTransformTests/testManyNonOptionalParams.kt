import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun Test(a: Int, b: Boolean, c: Int = 0, d: Foo = Foo(), e: List<Int> = emptyList()) {
    A(a, b, c, d, e)
}
