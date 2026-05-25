import androidx.compose.runtime.*

@Composable
fun Test(content: (@Composable () -> Unit?)) {
    content?.let { it() }
}
