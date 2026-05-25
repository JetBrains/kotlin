import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test(a: Int) {
    val b = someInt()
    val foo = remember(a, b) { Foo(a, b) }
}
