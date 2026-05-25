import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int) {
    // result blocks have composable calls, so we generate groups round them. It's a
    // statically guaranteed number of groups at execution, so no wrapping group is
    // needed.
    when {
        x < 0 -> A(a)
        x > 30 -> NA()
        else -> A(b)
    }
}
