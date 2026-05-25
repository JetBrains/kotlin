import androidx.compose.runtime.*

val state by mutableStateOf(true)

@Composable
fun getCondition() = remember { mutableStateOf(false) }.value

@Composable
fun ReceiveValue(value: Int) { }
