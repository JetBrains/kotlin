import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun SimpleBox(modifier: Modifier = Modifier, content: @Composable() () -> Unit = {}) {
    used(modifier)
    content()
}
