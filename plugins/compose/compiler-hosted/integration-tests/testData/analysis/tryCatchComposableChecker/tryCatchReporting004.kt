// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable fun foo() { }

@Composable fun bar() {
    <!ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE!>try<!> {
        (1..10).forEach { foo() }
    } catch(e: Exception) {
    }
}
