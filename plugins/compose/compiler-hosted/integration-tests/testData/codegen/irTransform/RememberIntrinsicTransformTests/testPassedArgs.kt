import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun rememberFoo(a: Int, b: Int) = remember(a, b) { Foo(a, b) }
