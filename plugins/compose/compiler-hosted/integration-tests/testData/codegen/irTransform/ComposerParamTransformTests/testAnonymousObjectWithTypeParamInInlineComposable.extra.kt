import androidx.compose.runtime.*

interface StateCell<T>

@Composable
inline fun <T> scope(block: @Composable () -> T): T = block()
