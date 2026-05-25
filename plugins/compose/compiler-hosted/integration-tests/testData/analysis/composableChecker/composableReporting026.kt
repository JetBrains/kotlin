// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun Leaf() {}

@Composable
fun Group(content: @Composable () -> Unit) { content() }

@Composable
fun foo() {
    Group {
        Leaf()
    }
}
