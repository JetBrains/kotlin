import androidx.compose.runtime.Composable

@Composable fun A() {
    val foo = @Composable {}
    val bar: @Composable () -> Unit = {}
    B(foo)
    B(bar)
}
