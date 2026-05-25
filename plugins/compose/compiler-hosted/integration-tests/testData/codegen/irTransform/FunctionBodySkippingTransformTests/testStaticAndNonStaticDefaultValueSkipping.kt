import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun Example(
    wontChange: Int = 123,
    mightChange: Int = LocalColor.current
) {
    A(wontChange)
    A(mightChange)
}
