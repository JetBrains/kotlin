// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable fun foo() { }

@Composable fun bar() {
    <!ILLEGAL_RUN_CATCHING_AROUND_COMPOSABLE!>runCatching<!> {
        (1..10).forEach { foo() }
    }
}
