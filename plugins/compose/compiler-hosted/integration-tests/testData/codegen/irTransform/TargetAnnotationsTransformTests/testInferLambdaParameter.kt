import androidx.compose.runtime.Composable

@Composable
fun Test(content: @Composable () -> Unit) {
  Row {
    Text("test")
  }
}
