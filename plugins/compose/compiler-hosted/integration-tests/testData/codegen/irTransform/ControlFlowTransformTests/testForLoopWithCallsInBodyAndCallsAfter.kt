import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(items: List<Int>) {
    // The composable call is made a conditional number of times, so we need to wrap
    // the loop with a dynamic wrapping group.
    for (i in items) {
        P(i)
    }
    A()
}
