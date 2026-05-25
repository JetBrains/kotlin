import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun Test(inlineInt: InlineInt) {
    val a = InlineInt(123)
    val foo = remember(inlineInt, a) { Foo() }
}
