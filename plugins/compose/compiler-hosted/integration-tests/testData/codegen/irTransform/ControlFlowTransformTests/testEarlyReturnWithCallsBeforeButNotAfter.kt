import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int) {
    // in the early return path, we need only close out the opened groups
    if (x > 0) {
        A()
        return
    }
    print("hello")
}
