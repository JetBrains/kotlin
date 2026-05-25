import androidx.compose.runtime.Composable

@Composable
inline fun Layout(content: @Composable () -> Unit) { content() }

@Composable
fun Text(text: String) { }
