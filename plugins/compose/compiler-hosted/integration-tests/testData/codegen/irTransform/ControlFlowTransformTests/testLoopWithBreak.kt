import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(a: Iterator<Int>, b: Iterator<Int>) {
    a@while (a.hasNext()) {
        val x = a.next()
        b@while (b.hasNext()) {
            val y = b.next()
            if (y == x) {
                break@a
            }
            A()
        }
        A()
    }
}
