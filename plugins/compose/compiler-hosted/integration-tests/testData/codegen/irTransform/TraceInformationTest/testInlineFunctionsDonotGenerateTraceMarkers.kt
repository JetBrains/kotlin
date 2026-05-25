import androidx.compose.runtime.*

@Composable
inline fun Wrapper(content: @Composable () -> Unit) = content()

@Composable
fun Test(condition: Boolean) {
    A()
    Wrapper {
        A()
        if (!condition) return
        A()
    }
    A()
}
