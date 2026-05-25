import androidx.compose.runtime.*

@Composable
private fun Test(param: String?) {
    val state = remember { mutableStateOf(false) }
    when (state.value) {
        true -> return Text(text = "true")
        else -> Text(text = "false")
    }
}
