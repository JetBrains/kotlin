import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int) {
    // no need for a group around the when expression overall, but since the result
    // of the expression is now being used, we need to generate temporary variables to
    // capture the result but still do the execution of the expression inside of groups.
    val y = when (x) {
        0 -> R(a)
        1 -> R(b)
        else -> R(c)
    }
}
