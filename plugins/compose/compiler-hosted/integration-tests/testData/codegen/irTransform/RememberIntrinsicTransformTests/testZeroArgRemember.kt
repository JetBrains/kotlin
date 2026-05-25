import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test(items: List<Int>) {
    val foo = remember { Foo() }
    used(items)
}
