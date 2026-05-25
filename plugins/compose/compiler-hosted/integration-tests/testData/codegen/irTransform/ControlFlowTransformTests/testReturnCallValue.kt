import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable @Composable
fun Example(): Int {
    // since the return expression is a composable call, we need to generate a
    // temporary variable and then return it after ending the open groups.
    A()
    return R()
}
