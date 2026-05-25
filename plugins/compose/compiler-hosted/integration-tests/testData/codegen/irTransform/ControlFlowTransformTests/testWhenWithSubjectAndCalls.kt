import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(x: Int) {
    // calls only in the result block, which means we can statically guarantee the
    // number of groups, so no group around the when is needed, just groups around the
    // result blocks.
    when (x) {
        0 -> A(a)
        1 -> A(b)
        else -> A(c)
    }
}
