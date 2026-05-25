// RUN_PIPELINE_TILL: FRONTEND

@file:ComposableTarget("N")

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableTarget

@Composable @ComposableTarget("N") fun N() {}
@Composable @ComposableTarget("M") fun M() {}

@Composable
fun AssumesN() {
    <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
}
