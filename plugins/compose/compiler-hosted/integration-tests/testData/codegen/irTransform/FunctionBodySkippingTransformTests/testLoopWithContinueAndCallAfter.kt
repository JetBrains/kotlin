import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
@NonRestartableComposable
fun Example() {
    Call()
    for (index in 0..1) {
        Call()
        if (condition())
            continue
        Call()
    }
}
