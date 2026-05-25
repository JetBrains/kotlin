import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Composable

@NonRestartableComposable
@Composable
fun Err() {
    // `x` is not a capture of handler, but is treated as such.
    fun handler() {
        { x: Int -> x }
    }
    // Lambda calling handler. To find captures, we need captures of `handler`.
    {
      handler()
    }
}
