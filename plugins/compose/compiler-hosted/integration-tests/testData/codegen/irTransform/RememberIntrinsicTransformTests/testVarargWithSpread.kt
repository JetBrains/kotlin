import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test(items: Array<Bar>) {
    val foo = remember(*items) { Foo() }
}
