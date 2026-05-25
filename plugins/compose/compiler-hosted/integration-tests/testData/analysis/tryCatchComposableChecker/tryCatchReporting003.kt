// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable fun foo() { }

@Composable fun bar() {
    try {
    } catch(e: Exception) {
        foo()
    } finally {
        foo()
    }
}
