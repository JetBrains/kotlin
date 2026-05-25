import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int) {
    // composable calls are in the condition blocks of the when statement. Since these
    // are conditionally executed, we can't statically know the number of groups during
    // execution. as a result, we must wrap the when clause with a group. Since there
    // are no other composable calls, the function body group will suffice.
    when {
        x == R(a) -> NA()
        x > R(b) -> NA()
        else -> NA()
    }
}
