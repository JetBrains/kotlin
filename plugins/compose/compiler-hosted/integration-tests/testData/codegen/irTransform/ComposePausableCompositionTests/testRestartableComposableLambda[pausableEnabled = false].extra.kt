import androidx.compose.runtime.*

fun use(value: Any?) { println(value) }
@Composable fun Wrap(content: @Composable () -> Unit) = content()
