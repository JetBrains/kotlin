// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
fun T() {
    N()
    <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
}

@Composable
@ComposableTarget("N")
fun N() {}

@Composable
@ComposableTarget("M")
fun M() {}
