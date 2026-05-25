// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

fun interface A { fun f() }
@Composable fun B() {
  A {
    <!COMPOSABLE_INVOCATION!>B<!>()
  }
}
