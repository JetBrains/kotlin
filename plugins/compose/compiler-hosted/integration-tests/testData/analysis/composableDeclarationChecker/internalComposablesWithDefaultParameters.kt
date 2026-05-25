// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable
internal abstract class A {
    @Composable open fun foo(x: Int = 0) {}
    @Composable abstract fun bar(x: Int = 0)
}
