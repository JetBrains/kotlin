import androidx.compose.runtime.*

fun Defer(content: @Composable () -> Unit) { }

fun UiContent(content: @Composable @ComposableTarget("UI") () -> Unit) { }
