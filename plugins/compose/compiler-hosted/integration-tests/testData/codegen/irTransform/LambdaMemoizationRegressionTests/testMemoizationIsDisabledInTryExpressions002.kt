import androidx.compose.runtime.*

fun test() {
    try {
        val f = @Composable { if (true) foo(block = {}) }
    } finally {}
}
