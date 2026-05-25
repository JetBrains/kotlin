// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
@Composable fun C(): Int { return 123 }
val <!COMPOSABLE_EXPECTED!>ncProp<!>: Int get() = <!COMPOSABLE_INVOCATION!>C<!>()
