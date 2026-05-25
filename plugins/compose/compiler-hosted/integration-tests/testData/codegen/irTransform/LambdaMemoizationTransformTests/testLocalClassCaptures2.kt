import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable

@NonRestartableComposable
@Composable
fun Example(z: Int) {
    class Foo(val x: Int) { val y = z }
    val lambda: () -> Any = {
        Foo(1)
    }
}
