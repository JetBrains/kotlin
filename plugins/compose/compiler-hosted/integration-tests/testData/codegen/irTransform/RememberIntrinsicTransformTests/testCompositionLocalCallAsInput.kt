import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test() {
    val foo = remember(compositionLocalBar.current) { Foo() }
}
