// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
@Composable fun A() {}

@Composable
fun test() {
    try {
        object {
            val x: Int
                @Composable get() = <!INLINE_FROM_HIGHER_PLATFORM!>remember<!> { 0 }
        }
    } finally {}
}
