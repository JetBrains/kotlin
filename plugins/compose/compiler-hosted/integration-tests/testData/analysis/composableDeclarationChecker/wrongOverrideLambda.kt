// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

class Impl : () -> Unit {
    @Composable override <!CONFLICTING_OVERLOADS!>fun invoke()<!> {}
}
