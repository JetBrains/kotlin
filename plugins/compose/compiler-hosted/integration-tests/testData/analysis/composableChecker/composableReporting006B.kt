// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun Leaf() {}

@Composable
fun foo() {
    val bar = @Composable {
        Leaf()
    }
    bar()
    System.out.println(bar)
}
