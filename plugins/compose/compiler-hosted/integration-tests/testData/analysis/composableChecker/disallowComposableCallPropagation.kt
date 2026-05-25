// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
class Foo
@Composable inline fun a(block1: @DisallowComposableCalls () -> Foo): Foo {
    return block1()
}
@Composable inline fun b(<!MISSING_DISALLOW_COMPOSABLE_CALLS_ANNOTATION!>block2: () -> Foo<!>): Foo {
  return a { block2() }
}
@Composable inline fun c(block2: @DisallowComposableCalls () -> Foo): Foo {
  return a { block2() }
}
