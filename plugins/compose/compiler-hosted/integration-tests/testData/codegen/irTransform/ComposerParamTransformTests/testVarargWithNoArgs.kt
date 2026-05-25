@file:OptIn(
  InternalComposeApi::class,
)
package test

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable


@Composable
fun VarArgsFirst(vararg foo: Any?) {
    println(foo)
}

@Composable
fun VarArgsCaller() {
    VarArgsFirst()
}
