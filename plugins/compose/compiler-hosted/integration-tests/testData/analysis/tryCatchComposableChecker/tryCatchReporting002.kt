// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

fun foo() { }

@Composable fun bar() {
    try {
        foo()
    } catch(e: Exception) {
    }
}
