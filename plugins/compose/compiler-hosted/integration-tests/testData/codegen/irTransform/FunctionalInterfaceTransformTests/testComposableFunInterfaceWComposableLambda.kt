import androidx.compose.runtime.*

@Composable
private fun Decorated(boolean: Boolean) {
    Decoratable(decorator = { content ->
        used(boolean)
        content()
    })
}

@Composable
private fun Decoratable(decorator: Decoration) {
    decorator.Decoration {

    }
}

fun interface Decoration {
    @Composable fun Decoration(content: @Composable () -> Unit)
}
