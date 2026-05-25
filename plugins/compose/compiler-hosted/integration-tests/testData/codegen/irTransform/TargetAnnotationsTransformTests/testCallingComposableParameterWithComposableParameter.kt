import androidx.compose.runtime.*

@Composable
fun Test(decorator: @Composable (content: @Composable () -> Unit) -> Unit) {
    decorator {
      Text("Some text")
    }
}
