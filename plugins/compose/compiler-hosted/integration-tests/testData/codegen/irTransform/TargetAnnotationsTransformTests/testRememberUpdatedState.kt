import androidx.compose.runtime.*

@Composable
fun Test(content: @Composable () -> Unit) {
    val updatedContent by rememberUpdatedState(content)
    Defer {
        UiContent {
            updatedContent()
        }
    }
}
