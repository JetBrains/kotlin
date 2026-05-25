import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable fun MaybeStable.example(x: Int) {
    used(this)
    used(x)
}
val example: @Composable MaybeStable.(Int) -> Unit = {
    used(this)
    used(it)
}
