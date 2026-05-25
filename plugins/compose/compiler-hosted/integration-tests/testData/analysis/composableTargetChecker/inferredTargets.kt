// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
fun A() {
  N()
}

@Composable
fun B() {
  N()
}

@Composable
fun C() {
  A()
  B()
}

@Composable
@ComposableTarget("N")
fun N() { }
