// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

class Impl : @Composable () -> Unit {
    override <!CONFLICTING_OVERLOADS!>fun invoke()<!> {}
}
