import androidx.compose.runtime.*

fun test() {
    val f = @Composable {
        try {
            if (true) foo(block = {})
        } finally {}
    }
}
