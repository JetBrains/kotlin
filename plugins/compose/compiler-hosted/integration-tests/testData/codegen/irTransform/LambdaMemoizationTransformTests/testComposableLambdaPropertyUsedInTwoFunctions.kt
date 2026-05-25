import androidx.compose.runtime.*
val a = @Composable { }
val b = @Composable { }

@Composable fun Foo() {
    a()
    b()
}

@Composable inline fun Bar() {
    a()
    b()
}
