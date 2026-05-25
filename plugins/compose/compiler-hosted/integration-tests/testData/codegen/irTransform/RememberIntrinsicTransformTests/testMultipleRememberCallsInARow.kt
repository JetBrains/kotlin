import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test() {
    val a = someInt()
    val b = someInt()
    val foo = remember(a, b) { Foo(a, b) }
    val c = someInt()
    val d = someInt()
    val bar = remember(c, d) { Foo(c, d) }
}
