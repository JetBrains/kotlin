// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
@ComposableTarget("N")
fun T() {
    <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
}

@Composable
@ComposableTarget("M")
fun M() {}
