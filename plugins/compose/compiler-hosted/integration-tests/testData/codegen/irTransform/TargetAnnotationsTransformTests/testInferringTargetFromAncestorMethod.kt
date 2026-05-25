import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableTarget
import androidx.compose.runtime.ComposableOpenTarget

@Composable @ComposableOpenTarget(0) fun OpenTarget() { }

abstract class Base {
  @Composable @ComposableTarget("N") abstract fun Compose()
}

class Valid : Base () {
  @Composable override fun Compose() {
    OpenTarget()
  }
}
