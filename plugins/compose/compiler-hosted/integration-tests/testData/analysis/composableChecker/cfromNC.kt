// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable fun C() {}
fun <!COMPOSABLE_EXPECTED!>NC<!>() { <!COMPOSABLE_INVOCATION!>C<!>() }
