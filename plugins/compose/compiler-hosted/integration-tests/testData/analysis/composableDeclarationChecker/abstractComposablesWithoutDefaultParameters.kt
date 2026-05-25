// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable
abstract class A {
    @Composable abstract fun foo(x: Int)
}
