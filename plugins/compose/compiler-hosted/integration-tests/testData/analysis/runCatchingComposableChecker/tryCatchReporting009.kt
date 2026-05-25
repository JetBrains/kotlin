// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
@Composable fun A() {}

@Composable
fun test() {
    runCatching {
        class C {
            init { <!COMPOSABLE_INVOCATION!>A<!>() }
        }
    }
}
