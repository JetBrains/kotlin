import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example() {
    // A while loop's condition block gets executed a conditional number of times, so
    // so we must generate a group around the while expression overall.
    while (B()) {
        print("hello world")
    }
    A()
}
