import androidx.compose.runtime.Composable

@Composable
fun Test(z: Int, x: Int, y: Result<Int>) {
    Wrapper { Test(z, x, y) }
}

@Composable
fun Wrapper(content: @Composable () -> Unit) { content() }
