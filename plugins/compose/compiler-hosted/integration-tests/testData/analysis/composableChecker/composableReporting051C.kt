// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

class A {
    val bar @Composable get() = 123
}

val A.bam @Composable get() = 123

@Composable
fun App() {
    val a = A()
    a.bar
    a.bam
    with(a) {
        bar
        bam
    }
}
