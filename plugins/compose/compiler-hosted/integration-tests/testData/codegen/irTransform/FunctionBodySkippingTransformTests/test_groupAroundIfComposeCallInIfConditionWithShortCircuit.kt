import androidx.compose.runtime.*

@Composable
fun Test() {
    ReceiveValue(if (state && getCondition()) 0 else 1)
}
