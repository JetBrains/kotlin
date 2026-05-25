import androidx.compose.runtime.*

fun runApplication(child: @Composable () -> Unit) {
    /* Pretend to be an entry point */
}

fun Foo() {
    var state = 255
    runApplication {
        println("$state")
    }
}

fun used(x: Any?) {}
