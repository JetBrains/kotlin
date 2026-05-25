import androidx.compose.runtime.*

@Composable
fun Widget(content: (@Composable () -> Unit)?) {
    if (content != null) content()
}
