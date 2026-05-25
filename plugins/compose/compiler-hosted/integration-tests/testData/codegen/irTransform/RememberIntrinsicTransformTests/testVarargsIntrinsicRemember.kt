import androidx.compose.runtime.*

@Composable
fun Test(vararg strings: String) {
    val show = remember { mutableStateOf(false) }
    if (show.value) {
        Text("Showing")
    }
}
