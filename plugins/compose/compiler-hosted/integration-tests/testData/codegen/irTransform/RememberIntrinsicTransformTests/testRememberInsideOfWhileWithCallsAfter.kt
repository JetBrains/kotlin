import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test(items: List<Int>) {
    for (item in items) {
        val foo = remember { Foo() }
        A()
        print(foo)
        print(item)
    }
}
