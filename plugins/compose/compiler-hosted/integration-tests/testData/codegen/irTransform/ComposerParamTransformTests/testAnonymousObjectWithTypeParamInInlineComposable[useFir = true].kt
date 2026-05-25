import androidx.compose.runtime.*

@Composable
fun <T> state(): StateCell<T> {
    return scope {
        object : StateCell<T> {}
    }
}

fun used(x: Any?) {}
