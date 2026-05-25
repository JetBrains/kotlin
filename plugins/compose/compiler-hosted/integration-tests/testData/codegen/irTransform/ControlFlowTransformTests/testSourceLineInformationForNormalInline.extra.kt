import androidx.compose.runtime.Composable

@Composable fun W(block: @Composable () -> Unit) = block()
@Composable inline fun IW(block: @Composable () -> Unit) = block()
@Composable fun T(value: Int) { }
