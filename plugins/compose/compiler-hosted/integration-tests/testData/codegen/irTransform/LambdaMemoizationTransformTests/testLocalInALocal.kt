import androidx.compose.runtime.Composable

@Composable fun Example() {
    @Composable fun A() { }
    @Composable fun B(content: @Composable () -> Unit) { content() }
    @Composable fun C() {
        B { A() }
    }
}
