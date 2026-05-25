import androidx.compose.runtime.Composable


@Composable fun A(x: Int = 0, y: Int = 0) {}
@Composable fun Wrap(content: @Composable () -> Unit) {
    content()
}

fun used(x: Any?) {}
