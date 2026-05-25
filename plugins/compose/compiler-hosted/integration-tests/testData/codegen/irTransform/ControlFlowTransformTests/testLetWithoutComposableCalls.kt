import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@Composable
fun Example(x: Int?) {
  x?.let {
    if (it > 0) {
      NA()
    }
    NA()
  }
  A()
}
