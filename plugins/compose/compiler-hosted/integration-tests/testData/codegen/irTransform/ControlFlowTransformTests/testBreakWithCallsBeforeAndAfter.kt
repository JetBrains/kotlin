import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(items: Iterator<Int>) {
    // a group around while is needed here, but the function body group will suffice
    while (items.hasNext()) {
        val i = items.next()
        val j = i
        P(i)
        if (i == 0) {
            break
        }
        P(j)
    }
}
