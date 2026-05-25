import androidx.compose.runtime.Composable

@Composable
fun TestLambda(content: () -> Unit) {
  content()
}

@Composable
fun Test(a: String) {
  TestLambda {
    println("Captures a" + a)
  }
}
