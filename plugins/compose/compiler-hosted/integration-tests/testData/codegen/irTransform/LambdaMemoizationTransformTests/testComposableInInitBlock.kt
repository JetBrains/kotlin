import androidx.compose.runtime.*

fun setContent(content: @Composable () -> Unit) {}

class ComposeScreenSaverView {
    init {
        val specsInit = 10
        val prefsInit by mutableStateOf(11)

        setContent {
            val imgLoaderInit = remember(prefsInit, specsInit) {
                123
            }
        }
    }
}
