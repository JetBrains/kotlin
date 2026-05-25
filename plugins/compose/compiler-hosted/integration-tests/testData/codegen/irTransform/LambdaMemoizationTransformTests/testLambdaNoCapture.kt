import androidx.compose.runtime.Composable

@Composable
fun TestLambda(content: () -> Unit) {
  content()
}

@Composable
fun Test() {
  TestLambda {
    println("Doesn't capture")
  }
}
