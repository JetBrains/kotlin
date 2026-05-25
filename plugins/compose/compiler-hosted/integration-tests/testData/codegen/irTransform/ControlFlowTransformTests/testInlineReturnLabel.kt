import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@Composable
@NonRestartableComposable
fun CustomTextBroken(condition: Boolean) {
    FakeBox {
        if (condition) {
            return@FakeBox
        }
        A()
    }
}
@Composable
inline fun FakeBox(content: @Composable () -> Unit) {
    content()
}
