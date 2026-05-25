import androidx.compose.runtime.*

fun test() {
    val f = @Composable {
        try {
            repeat (3) {
                if (true) foo(block = {})
            }
        } finally {}
    }
}
