// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

@Composable fun A() {}

val aCallable: () -> Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> ::A
val bCallable: @Composable () -> Unit = ::A
val cCallable = ::A

fun doSomething(fn: () -> Unit) { }

@Composable fun B(content: @Composable () -> Unit) {
    content()
    doSomething(::<!INAPPLICABLE_CANDIDATE!>A<!>)
    doSomething(aCallable)
    doSomething(<!ARGUMENT_TYPE_MISMATCH!>bCallable<!>)
    doSomething(<!ARGUMENT_TYPE_MISMATCH!>cCallable<!>)
    B(::A)
    B(<!ARGUMENT_TYPE_MISMATCH!>aCallable<!>)
    B(bCallable)
    B(cCallable)
}
