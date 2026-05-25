// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun foo() {
    val bar = @Composable {}
    bar()
    System.out.println(bar)
}
