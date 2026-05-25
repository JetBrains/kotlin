import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example() {
    // The for loop's subject expression is only executed once, so we don't need any
    // additional groups
    for (i in L()) {
        print(i)
    }
}
