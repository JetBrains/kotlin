// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

var x: (@Composable () -> Unit)? = null

class Foo
fun Foo.setContent(content: @Composable () -> Unit) {
    x = content
}

@Composable
fun Leaf() {}

fun Example(foo: Foo) {
    foo.setContent { Leaf() }
}
