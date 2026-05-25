// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

data class Foo(val value: Int)

@Composable fun A(x: Foo) { println(x) }

@Composable fun Test() {
    val x = Foo(123)

    // NOTE: It's important that the diagnostic be only over the attribute key, so that
    // we don't make a large part of the elements red when the type is otherwise correct
    A(x=x, <!ARGUMENT_PASSED_TWICE!>x<!>=x)
}
