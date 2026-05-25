import androidx.compose.runtime.*

@Composable
fun Wrap(content: @Composable () -> Unit) = content()

fun used(value: Any) { }
