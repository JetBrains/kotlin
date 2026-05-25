// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable fun Foo() {}

val <!COMPOSABLE_EXPECTED!>y<!>: Any get() =
<!COMPOSABLE_INVOCATION, INLINE_FROM_HIGHER_PLATFORM!>remember<!> { mutableStateOf(1) }

fun App() {
    val x = object {
      val <!COMPOSABLE_EXPECTED!>a<!> get() =
      <!COMPOSABLE_INVOCATION, INLINE_FROM_HIGHER_PLATFORM!>remember<!> { mutableStateOf(2) }
      val c @Composable get() = <!INLINE_FROM_HIGHER_PLATFORM!>remember<!> { mutableStateOf(4) }
      @Composable fun bar() { Foo() }
      fun <!COMPOSABLE_EXPECTED!>foo<!>() {
        <!COMPOSABLE_INVOCATION!>Foo<!>()
      }
    }
    class Bar {
      val <!COMPOSABLE_EXPECTED!>b<!> get() =
      <!COMPOSABLE_INVOCATION, INLINE_FROM_HIGHER_PLATFORM!>remember<!> { mutableStateOf(6) }
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
