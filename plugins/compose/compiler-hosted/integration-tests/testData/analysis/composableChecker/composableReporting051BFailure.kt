// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

class A {
    <!WRONG_ANNOTATION_TARGET!>@Composable<!> val bar get() = 123
}

<!WRONG_ANNOTATION_TARGET!>@Composable<!> val A.bam get() = 123

fun App() {
    val a = A()
    a.bam
}
