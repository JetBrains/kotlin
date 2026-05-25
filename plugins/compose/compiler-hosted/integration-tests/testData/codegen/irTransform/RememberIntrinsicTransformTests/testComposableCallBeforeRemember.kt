import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test() {
    A()
    val foo = remember { Foo() }
}
