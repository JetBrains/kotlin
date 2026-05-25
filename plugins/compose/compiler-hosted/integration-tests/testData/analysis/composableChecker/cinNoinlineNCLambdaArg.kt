// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
@Composable fun C() { }
<!NOTHING_TO_INLINE!>inline<!> fun NoinlineNC(noinline lambda: () -> Unit) { lambda() }
@Composable fun C3() {
    NoinlineNC {
        <!COMPOSABLE_INVOCATION!>C<!>()
    }
}
