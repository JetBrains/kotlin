// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableTarget

@Composable @ComposableTarget("N") fun N() { }
@Composable @ComposableTarget("M") fun M() { }

abstract class Base {
  @Composable @ComposableTarget("N") abstract fun Compose()
}

class Invalid : Base() {
  @Composable override fun Compose() {
    <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
  }
}

class Valid : Base () {
  @Composable override fun Compose() {
    N()
  }
}
