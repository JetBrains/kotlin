import androidx.compose.runtime.*

@Composable
inline fun Inline1(block: @Composable () -> Unit) {
    block()
}

@Composable
inline fun Inline2(block: @Composable () -> Unit) {
    block()
}
