import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember

@NonRestartableComposable
@Composable
fun Example() {
    val x = remember { Unstable() }
    val shouldNotMemoize = x::foo
}
