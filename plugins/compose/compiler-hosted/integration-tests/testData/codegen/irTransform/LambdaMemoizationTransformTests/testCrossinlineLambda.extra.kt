import androidx.compose.runtime.Composable
@Composable fun Text(s: String) {}
inline fun f(crossinline block: (String) -> Unit) = block("")
