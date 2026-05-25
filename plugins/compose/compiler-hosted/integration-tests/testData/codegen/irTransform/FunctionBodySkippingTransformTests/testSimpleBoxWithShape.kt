import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun SimpleBox(modifier: Modifier = Modifier, shape: Shape = RectangleShape) {
    used(modifier)
    used(shape)
}
