import androidx.compose.runtime.*


private interface DefaultParamInterface {
    @Composable fun Content(
        content: @Composable () -> Unit = @Composable { ComposedContent { Text("default") } }
    )
    @Composable fun ComposedContent(
        content: @Composable () -> Unit = @Composable { Text("default") }
    ) {
        content()
    }
}

fun used(x: Any?) {}
