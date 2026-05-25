import androidx.compose.runtime.*

@Composable inline fun Column(content: @Composable () -> Unit) {}
inline fun NonComposable(content: () -> Unit) {}
@Composable fun Text(text: String) {}
