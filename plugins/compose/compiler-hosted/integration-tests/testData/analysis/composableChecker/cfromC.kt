// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable fun C() {}
@Composable fun C2() { C() }
