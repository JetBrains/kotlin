import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test(a: Int): Foo {
    val b = someInt()
    return remember(a, b) { Foo(a, b) }
}
