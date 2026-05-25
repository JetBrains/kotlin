import androidx.compose.runtime.*

val state by mutableStateOf(true)

@Composable fun getConditionA() = true
@Composable fun getConditionB() = true
@Composable fun getConditionC() = true

@Composable fun resultA() = true
@Composable fun resultB() = true
@Composable fun resultC() = true
@Composable fun resultS() = true

fun ReceiveValue(value: Int) { }
