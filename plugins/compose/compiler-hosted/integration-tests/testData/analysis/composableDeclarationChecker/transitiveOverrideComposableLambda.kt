// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

interface ComposableFunction : @Composable () -> Unit

class Impl : ComposableFunction {
    @Composable
    override fun invoke() {}
}
