import androidx.compose.runtime.*

@Composable
inline fun Fn(int: Int): Int = 0
@Composable
inline fun Fn2(int: Int): Int = 0
@Composable
inline fun Content(content: @Composable (Int) -> Int) { content(0) }
