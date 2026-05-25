// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable
open class A {
    @Composable open fun foo(x: Int = 0) {}
}
