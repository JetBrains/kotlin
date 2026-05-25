import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int?) {
    // The composable call is made conditionally, which means it is like an if, but with
    // only one result having a composable call, so we just generate a single group
    // around the whole expression.
    val y = x ?: R()
}
