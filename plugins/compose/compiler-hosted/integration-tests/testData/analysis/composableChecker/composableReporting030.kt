// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun foo() {
    val myVariable: @Composable ()->Unit = {};
    myVariable()
}
