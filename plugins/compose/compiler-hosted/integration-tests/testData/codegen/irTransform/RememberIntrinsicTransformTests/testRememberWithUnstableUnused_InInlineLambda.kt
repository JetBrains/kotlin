import androidx.compose.runtime.*

class Unstable(var x: Int)

@Composable fun Test(param: String, unstable: Unstable) {
    InlineWrapper {
        remember(param) { param }
    }
}

fun used(x: Any?) {}
