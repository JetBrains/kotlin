import androidx.compose.runtime.Composable

@Composable fun Lazy(content: () -> Unit) {}
@Composable inline fun Box(content: () -> Unit) {}
