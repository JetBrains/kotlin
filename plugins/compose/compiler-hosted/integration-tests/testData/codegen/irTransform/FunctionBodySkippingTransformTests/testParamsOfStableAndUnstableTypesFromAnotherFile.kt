import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
internal fun Test(stable: Stable, unstable: Unstable) {
    used(stable)
    used(unstable)
}
