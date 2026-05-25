import androidx.compose.runtime.*

@Composable
fun Test() {
    ReceiveValue(if (when {
      getConditionA() -> resultA()
      getConditionB() -> resultB()
      getConditionC() -> resultC()
      state -> resultS()
      else -> false
    }) 1 else 0)
}
