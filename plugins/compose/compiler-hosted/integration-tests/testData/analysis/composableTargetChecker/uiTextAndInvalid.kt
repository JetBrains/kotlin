// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableTarget
import androidx.compose.foundation.text.BasicText

@Composable @ComposableTarget("N")
fun Invalid() { }

@Composable
fun UseText() {
   BasicText("Some text")
   <!COMPOSE_APPLIER_CALL_MISMATCH!>Invalid<!>()
}
