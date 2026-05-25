// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun MyComposable(content: @Composable ()->Unit) { content() }

@Composable
fun Leaf() {}

@Composable
fun foo() {
    MyComposable(content={ Leaf() })
}
