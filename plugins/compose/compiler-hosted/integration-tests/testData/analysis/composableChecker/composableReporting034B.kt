// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

fun identity(f: ()->Unit): ()->Unit { return f; }

@Composable
fun test(f: @Composable ()->Unit) {
    val f2: @Composable ()->Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> identity (<!ARGUMENT_TYPE_MISMATCH!>f<!>);
    f2()
}
