import androidx.compose.runtime.*

@Composable
private fun Decorated(boolean: Boolean) {
    var something = boolean
    Decoratable(decorator = { content ->
        used(something)
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
