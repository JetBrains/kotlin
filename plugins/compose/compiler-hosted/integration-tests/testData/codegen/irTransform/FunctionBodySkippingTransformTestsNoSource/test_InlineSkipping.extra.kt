import androidx.compose.runtime.*

@Composable
inline fun InlineWrapperParam(content: @Composable (Int) -> Unit) {
    content(100)
}

@Composable
fun Text(text: String) { }
