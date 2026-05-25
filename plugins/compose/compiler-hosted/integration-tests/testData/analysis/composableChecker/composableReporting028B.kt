// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

fun foo(v: @Composable ()->Unit) {
    val myVariable: ()->Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> v
    myVariable()
}
