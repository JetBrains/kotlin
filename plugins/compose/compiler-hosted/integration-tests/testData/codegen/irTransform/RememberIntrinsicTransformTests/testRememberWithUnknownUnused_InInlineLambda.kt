import androidx.compose.runtime.*

@Composable fun Test(param: String, unknown: List<*>) {
    InlineWrapper {
        remember(param) { param }
    }
}
