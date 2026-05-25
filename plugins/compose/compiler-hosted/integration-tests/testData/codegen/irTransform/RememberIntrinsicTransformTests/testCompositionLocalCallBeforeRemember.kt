import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test() {
    val bar = compositionLocalBar.current
    val foo = remember(bar) { Foo() }
}
