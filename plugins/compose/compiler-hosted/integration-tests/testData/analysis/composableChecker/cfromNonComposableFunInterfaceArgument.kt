// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

fun interface A { fun f() }

@Composable fun B(a: (A) -> Unit) {
  a {
    <!COMPOSABLE_INVOCATION!>B<!>(a)
  }
}
