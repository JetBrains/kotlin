import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@Composable
fun Test(condition: Boolean) {
    IW iw@ {
        if (condition) return@iw
        A()
    }
}
