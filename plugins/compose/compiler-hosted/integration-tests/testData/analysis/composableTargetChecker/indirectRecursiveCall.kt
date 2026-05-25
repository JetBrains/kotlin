// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable @ComposableTarget("N") fun N() { }

@Composable
fun T() {
  A(10) { N() }
}

@Composable
fun A(level: Int, content: @Composable () -> Unit) {
   if (level > 0) B(level - 1) { content() }
}

@Composable
fun B(level: Int, content: @Composable () -> Unit) {
  A(level) {
    content()
    B(level - 1) { content() }
  }
  N()
}
