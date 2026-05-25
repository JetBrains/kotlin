// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable fun Foo() {}

@Composable fun App() {
    val x = object {
      val <!COMPOSABLE_EXPECTED!>a<!> get() = <!COMPOSABLE_INVOCATION, INLINE_FROM_HIGHER_PLATFORM!>remember<!> { mutableStateOf(2) }
      val c @Composable get() = <!INLINE_FROM_HIGHER_PLATFORM!>remember<!> { mutableStateOf(4) }
      fun <!COMPOSABLE_EXPECTED!>foo<!>() {
        <!COMPOSABLE_INVOCATION!>Foo<!>()
      }
      @Composable fun bar() { Foo() }
    }
    class Bar {
      val <!COMPOSABLE_EXPECTED!>b<!> get() = <!COMPOSABLE_INVOCATION, INLINE_FROM_HIGHER_PLATFORM!>remember<!> { mutableStateOf(6) }
      val c @Composable get() = <!INLINE_FROM_HIGHER_PLATFORM!>remember<!> { mutableStateOf(7) }
    }
    fun <!COMPOSABLE_EXPECTED!>Bam<!>() {
        <!COMPOSABLE_INVOCATION!>Foo<!>()
    }
    @Composable fun Boo() {
        Foo()
    }
    print(x)
}
