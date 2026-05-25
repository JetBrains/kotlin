import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test(a: Int = someInt()) {
    val foo = remember { Foo() }
    used(foo)
    used(a)
}
