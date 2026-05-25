@file:OptIn(
  InternalComposeApi::class,
)
package test

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable


@Composable inline fun Example(content: @Composable () -> Unit) {
    content()
}

@NonRestartableComposable
@Composable fun Test() {
    Example {}
}
