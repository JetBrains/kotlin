// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
@ComposableTarget("a")
fun A() {
  B()
}

@Composable
@ComposableTarget("a")
fun B() { }

@Composable
@ComposableTarget("a")
fun C() {
  B()
}
