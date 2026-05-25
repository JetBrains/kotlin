import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(a: Iterator<Int>, b: Iterator<Int>) {
    a@while (a.hasNext()) {
        val x = a.next()
        if (x == 0) {
            break
        }
        b@while (b.hasNext()) {
            val y = b.next()
            if (y == 0) {
                break
            }
            if (y == x) {
                break@a
            }
            if (y > 100) {
                return
            }
            A()
        }
        A()
    }
}
