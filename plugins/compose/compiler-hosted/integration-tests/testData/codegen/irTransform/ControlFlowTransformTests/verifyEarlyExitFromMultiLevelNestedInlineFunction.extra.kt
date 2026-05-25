import androidx.compose.runtime.*

@Composable
fun Text(value: String) { }

@Composable
inline fun InlineLinearA(content: @Composable () -> Unit) {
    content()
}

@Composable
inline fun InlineLinearB(content: @Composable () -> Unit) {
    content()
}
