import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test(a: Int, b: Int, c: Bar, d: Boolean) {
    val foo = remember(a, b, c, d) { Foo() }
}
