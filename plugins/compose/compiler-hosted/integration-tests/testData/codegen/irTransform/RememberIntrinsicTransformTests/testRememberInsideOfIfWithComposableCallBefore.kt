import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test(condition: Boolean) {
    if (condition) {
        A()
        val foo = remember { Foo() }
    }
}
