// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
@Composable fun A() {}

@Composable
fun test() {
    try {
        @Composable fun B() {
            A()
        }
    } finally {}
}
