import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(a: Iterator<Int>, b: Iterator<Int>) {
    while (a.hasNext()) {
        val x = a.next()
        if (x > 100) {
            return
        }
        A()
    }
}
