import androidx.compose.runtime.*

@Composable
@ComposableTarget("UI")
fun Leaf() { }

@Composable
fun Wrapper(content: @Composable () -> Unit) { content() }

// [0,[0],[0],[0],[0],[0],[0],[0]]
@Composable
fun Optional(
    one: @Composable () -> Unit = { },
    two: (@Composable () -> Unit)? = null,
    three: (@Composable () -> Unit)? = null,
    four: (@Composable () -> Unit)? = null,
    five: (@Composable () -> Unit)? = null,
    six: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    one()

    // Invoke through a ?.
    two?.invoke()

    // Invoke through a let
    three?.let { it() }

    // Invoke through a let test
    four?.let { four() }

    // Invoke through in an then block
    if (five != null)
        five()

    six?.let { it -> Wrapper(it) }

    content()
}

@Composable
fun UseOptional() {
    Optional {
        Leaf()
    }
}
