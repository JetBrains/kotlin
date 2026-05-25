// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
fun test() {
    <!ILLEGAL_RUN_CATCHING_AROUND_COMPOSABLE!>runCatching<!> {
        val x by <!INLINE_FROM_HIGHER_PLATFORM!>remember<!> { lazy { 0 } }
        print(x)
    }
}
