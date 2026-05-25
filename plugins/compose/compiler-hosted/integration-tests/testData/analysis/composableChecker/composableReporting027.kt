// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun Leaf() {}

@Composable
fun Group(content: @Composable () -> Unit) { content() }

@Composable
fun foo() {
    Group {
        listOf(1,2,3).forEach {
            Leaf()
        }
    }
}
