// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable fun App() {
    val x = object {
      val b = <!INLINE_FROM_HIGHER_PLATFORM!>remember<!> { mutableStateOf(3) }
    }
    val y = run {
        object {
          val b = <!INLINE_FROM_HIGHER_PLATFORM!>remember<!> { mutableStateOf(3) }
          val a = object {
            val b = <!INLINE_FROM_HIGHER_PLATFORM!>remember<!> { mutableStateOf(3) }
          }
        }
    }
    class Bar {
      val a = <!COMPOSABLE_INVOCATION, INLINE_FROM_HIGHER_PLATFORM!>remember<!> { mutableStateOf(5) }
    }
    print(x)
    print(y)
}
