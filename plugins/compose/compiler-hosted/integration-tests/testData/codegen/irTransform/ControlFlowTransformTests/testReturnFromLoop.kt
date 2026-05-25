import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(items: Iterator<Int>) {
    while (items.hasNext()) {
        val i = items.next()
        val j = i
        val k = i
        val l = i
        P(i)
        if (i == 0) {
            P(j)
            return
        } else {
            P(k)
        }
        P(l)
    }
}
