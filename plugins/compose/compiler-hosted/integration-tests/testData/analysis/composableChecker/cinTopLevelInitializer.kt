// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
@Composable fun C(): Int { return 123 }
val ncProp: Int = <!COMPOSABLE_INVOCATION!>C<!>()
