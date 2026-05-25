// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable fun Leaf() {}

fun <!COMPOSABLE_EXPECTED!>foo<!>() {
    val bar: @Composable ()->Unit = @Composable {
        Leaf()
    }
    <!COMPOSABLE_INVOCATION!>bar<!>()
    System.out.println(bar)
}
