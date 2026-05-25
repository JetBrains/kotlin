// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
fun W(content: @Composable () -> Unit) { content() }

@Composable
@ComposableTarget("N")
fun N() {}

@Composable
@ComposableTarget("M")
fun M() {}

@Composable
fun T() {
    W {
        N()
    }
    W {
        <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
    }
}
