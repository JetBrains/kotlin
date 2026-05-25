import androidx.compose.runtime.Composable

@Composable
fun Test(enabled: Boolean, content: @Composable () -> Unit = {
        Display("$enabled")
    }
) {
    Wrap(content)
}
