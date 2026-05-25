import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test(condition: Boolean) {
    A()
    if (condition) {
        val foo = remember { Foo() }
    }
}
