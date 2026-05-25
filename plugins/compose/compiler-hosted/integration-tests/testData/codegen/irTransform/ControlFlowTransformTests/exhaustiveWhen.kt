import androidx.compose.runtime.*

@Composable
fun <T> Test(alpha: Alpha<T>): Float =
    when (alpha) {
        is Alpha.A -> 0f
        is Alpha.B -> 1f
    }
