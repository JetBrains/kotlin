import androidx.compose.runtime.*

@Composable
fun Test() {
   ReceiveValue(if (getCondition() && state) 0 else 1)
}
