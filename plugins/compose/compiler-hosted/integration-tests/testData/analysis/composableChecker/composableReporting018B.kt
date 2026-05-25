// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun Leaf() {}

fun foo() {
    val myVariable: ()->Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> @Composable { Leaf() }
    System.out.println(myVariable)
}
