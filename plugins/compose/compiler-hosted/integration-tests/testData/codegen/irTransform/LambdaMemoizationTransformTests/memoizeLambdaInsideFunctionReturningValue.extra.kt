import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Composable
fun Consume(block: () -> Int): Int = block()

@Stable
class Foo {
    val value: Int = 0
}
