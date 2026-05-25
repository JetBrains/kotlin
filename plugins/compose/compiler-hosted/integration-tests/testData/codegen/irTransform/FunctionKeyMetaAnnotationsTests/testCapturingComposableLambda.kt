import androidx.compose.runtime.*

@Composable
fun Wrapper(child: @Composable () -> Unit) {
    child()
}

@Composable
fun Foo() {
    var state by remember { mutableStateOf(0) }
    Wrapper {
        println("$state")
    }
}

fun used(x: Any?) {}
