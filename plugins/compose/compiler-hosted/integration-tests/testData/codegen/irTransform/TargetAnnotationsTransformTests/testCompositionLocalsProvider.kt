import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun Test() {
  CompositionLocalProvider {
    Text("test")
  }
}
