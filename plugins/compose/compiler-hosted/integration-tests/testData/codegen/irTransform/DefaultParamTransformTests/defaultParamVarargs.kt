import androidx.compose.runtime.*

            @Composable
fun Test() {
    VarargWDefault()
    VarargWDefault(1, 2)
    VarargWContent { }
    VarargWContent(1, 2) { }
}

@Composable
fun VarargWDefault(vararg values: Int = intArrayOf()) {
    used(values)
}

@Composable
fun VarargWContent(vararg values: Int = intArrayOf(1), content: @Composable () -> Unit) {
    used(values)
}
