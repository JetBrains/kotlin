// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

class Impl : @Composable () -> Unit {
    @Composable
    override fun invoke() {}
}
