import androidx.compose.runtime.*

class Scope {
    fun compose(block: @Composable () -> Unit) { }
}

fun T(block: suspend Scope.() -> Unit) { }

@Composable
inline fun M1(block: @Composable () -> Unit) { block() }

@Composable
fun Text(text: String) { }
