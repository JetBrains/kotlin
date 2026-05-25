// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable fun foo() { }

@Composable fun bar() {
    <!ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE!>try<!> {
        foo()
    } catch(e: Exception) {
    }
}
