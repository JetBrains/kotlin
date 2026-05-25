@file:OptIn(
  InternalComposeApi::class,
)
package test

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable
@Composable
fun Wat() {}

@NonRestartableComposable
@Composable
fun Foo(x: Int) {
    Wat()
    @NonRestartableComposable
    @Composable fun goo() { Wat() }
    class Bar {
        @NonRestartableComposable
        @Composable fun baz() { Wat() }
    }
    goo()
    Bar().baz()
}
