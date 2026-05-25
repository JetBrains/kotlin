import androidx.compose.runtime.*

@Composable
fun Test() {
   ReceiveValue(if (getCondition()) 0 else 1)
}
