import androidx.compose.runtime.*

@Composable fun Foo() {
    Box {}
}

@Composable fun Foo(x: Int) {
    Box {}
}

@Composable fun Int.Foo() {
    Box {}
}

@Composable fun Foo(x: String) {
    Box {}
}
