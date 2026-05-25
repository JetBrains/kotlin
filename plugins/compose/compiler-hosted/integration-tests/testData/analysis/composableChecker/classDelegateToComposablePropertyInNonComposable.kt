// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

interface A

interface B {
    val property: A @Composable get() = TODO()
}

fun <!COMPOSABLE_EXPECTED!>Test<!>(b: B) {
    val a = object : A by b.<!COMPOSABLE_INVOCATION!>property<!> {}
    println(a)
}
