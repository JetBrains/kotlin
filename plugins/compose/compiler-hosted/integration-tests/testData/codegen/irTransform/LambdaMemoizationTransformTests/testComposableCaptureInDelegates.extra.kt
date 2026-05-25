import androidx.compose.runtime.Composable

interface Delegate {
    val content: @Composable () -> Unit
}

class Impl(override val content: @Composable () -> Unit) : Delegate
