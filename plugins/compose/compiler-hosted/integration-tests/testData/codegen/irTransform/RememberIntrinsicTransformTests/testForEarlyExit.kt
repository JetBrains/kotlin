import androidx.compose.runtime.*

@Composable
fun Test(condition: Boolean) {
    val value = remember { mutableStateOf(false) }
    if (!value.value && !condition) return
    val value2 = remember { mutableStateOf(false) }
    Text("Text ${value.value}, ${value2.value}")
}
