import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable fun SomeThing(content: @Composable() () -> Unit) { content() }

@Composable
fun Example() {
    SomeThing {
        val id = object {}
    }
}
