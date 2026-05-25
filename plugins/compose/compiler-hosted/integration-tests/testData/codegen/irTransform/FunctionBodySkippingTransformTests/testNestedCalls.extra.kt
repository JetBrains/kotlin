import androidx.compose.runtime.Composable


@Composable fun B(a: Int = 0, b: Int = 0, c: Int = 0) {}
@Composable fun Provide(content: @Composable (Int) -> Unit) {}

fun used(x: Any?) {}
