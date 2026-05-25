import androidx.compose.runtime.Composable

@Composable
fun Test(enabled: Boolean) {
    val content: @Composable () -> Unit = {
        Display("$enabled")
    }
    Wrap(content)
}
