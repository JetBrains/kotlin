// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
@Composable fun A() {}

@Composable
fun test() {
    <!ILLEGAL_RUN_CATCHING_AROUND_COMPOSABLE!>runCatching<!> {
        object {
            val x = A()
        }
    }
}
