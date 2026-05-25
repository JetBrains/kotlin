// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

class A
typealias B = () -> Unit

fun foo(vararg a: @Composable () -> Unit) {
    println(a)
}

fun bar(vararg s: <!COMPOSABLE_INAPPLICABLE_TYPE!>@Composable<!> String) {
    println(s)
}

fun Test() {
    val a: <!COMPOSABLE_INAPPLICABLE_TYPE!>@Composable<!> A = A()
    val b: <!COMPOSABLE_INAPPLICABLE_TYPE!>@Composable<!> B <!INITIALIZER_TYPE_MISMATCH!>=<!> {}
    val c: @Composable () -> Unit = {}
    val s: <!COMPOSABLE_INAPPLICABLE_TYPE!>@Composable<!> String = ""

    println(a)
    println(b)
    println(c)
    println(s)
}
