@file:OptIn(
  InternalComposeApi::class,
)
package test

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable


@Composable
fun B(vararg values: Int) {
    print(values)
}

@NonRestartableComposable
@Composable
fun Test() {
    B(0, 1, 2, 3)
}
