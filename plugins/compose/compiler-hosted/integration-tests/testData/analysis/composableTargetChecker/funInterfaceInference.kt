// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
fun W(content: @Composable () -> Unit) { content() }

@Composable
@ComposableTarget("N")
fun N() {}

@Composable
@ComposableTarget("M")
fun M() {}

fun interface CustomComposable {
  @Composable
  fun call()
}

@Composable
fun OpenCustom(content: CustomComposable) {
  content.call()
}

@Composable
fun ClosedCustom(content: CustomComposable) {
  N()
  content.call()
}

@Composable
fun UseOpen() {
  N()
  OpenCustom {
    N()
  }
}

@Composable
fun UseClosed() {
  N()
  ClosedCustom {
    N()
  }
}

@Composable
fun OpenDisagree() {
  OpenCustom {
    N()
  }
  <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
}

@Composable
fun ClosedDisagree() {
  ClosedCustom {
    N()
    <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
  }
  <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
}
