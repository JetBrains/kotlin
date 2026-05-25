import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int) {
    // No composable calls, so no group generated except for at function boundary
    if (x > 0) {
        NA()
    }
}
