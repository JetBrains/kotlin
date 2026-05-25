import androidx.compose.runtime.*

@Composable
fun App() {
    (@Composable {
        object {}
    })()
}

fun used(x: Any?) {}
