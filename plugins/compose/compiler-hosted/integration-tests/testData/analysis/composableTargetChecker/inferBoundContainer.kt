// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
fun Wrapper(content: @Composable ()->Unit) {
    N()
    content()
}

@Composable
fun A() {
  Wrapper {
      B()
  }
}

@Composable
fun B() {
   N()
}

@Composable
@ComposableTarget("N")
fun N() {}
