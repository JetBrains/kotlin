import androidx.compose.runtime.Composable

@Composable
inline fun Wrapper(content: @Composable () -> Unit) { }

@Composable
fun Leaf(default: Int = 0) {}
