import androidx.compose.runtime.*

@Composable
fun Wrapper(content: @Composable () -> Unit) = content()
