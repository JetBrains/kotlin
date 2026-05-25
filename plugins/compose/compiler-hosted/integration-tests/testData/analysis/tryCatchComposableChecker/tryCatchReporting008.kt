// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
fun test() {
    <!ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE!>try<!> {
        val x by <!INLINE_FROM_HIGHER_PLATFORM!>remember<!> { lazy { 0 } }
        print(x)
    } finally {}
}
