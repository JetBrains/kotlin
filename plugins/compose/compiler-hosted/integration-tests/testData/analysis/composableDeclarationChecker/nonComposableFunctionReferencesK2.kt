// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

fun A() {}

val aCallable: () -> Unit = ::A
val bCallable: @Composable () -> Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> ::A
val cCallable = ::A

fun doSomething(fn: () -> Unit) { print(fn) }

@Composable fun B(content: @Composable () -> Unit) {
    content()
    doSomething(::A)
    doSomething(aCallable)
    doSomething(<!ARGUMENT_TYPE_MISMATCH!>bCallable<!>)
    doSomething(cCallable)
    B(::<!INAPPLICABLE_CANDIDATE!>A<!>)
    B(<!ARGUMENT_TYPE_MISMATCH!>aCallable<!>)
    B(bCallable)
    B(<!ARGUMENT_TYPE_MISMATCH!>cCallable<!>)
}
