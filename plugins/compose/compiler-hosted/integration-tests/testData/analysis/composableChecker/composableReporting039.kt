// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

fun composeInto(l: @Composable ()->Unit) { System.out.println(l) }

fun Foo() {
    composeInto {
        Baz()
    }
}

fun Bar() {
    Foo()
}
@Composable fun Baz() {}
