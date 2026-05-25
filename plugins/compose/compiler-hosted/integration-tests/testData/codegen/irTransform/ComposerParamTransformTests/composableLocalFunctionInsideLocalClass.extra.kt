import androidx.compose.runtime.*

abstract class C {
    @Composable
    abstract fun Render()
}

@Composable fun Button(onClick: () -> Unit, content: @Composable () -> Unit) {}
