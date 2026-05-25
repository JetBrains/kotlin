import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Composable

@NonRestartableComposable
@Composable
fun Err(y: Int, z: Int) {
    class Local {
        val w = z
        fun something(x: Int): Int { return x + y + w }
    }
    {
      Local().something(2)
    }
}
