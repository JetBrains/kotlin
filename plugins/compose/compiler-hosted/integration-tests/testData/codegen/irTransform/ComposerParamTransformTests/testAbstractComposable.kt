@file:OptIn(
  InternalComposeApi::class,
)
package test

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable


abstract class BaseFoo {
    @NonRestartableComposable
    @Composable
    abstract fun bar()
}

class FooImpl : BaseFoo() {
    @NonRestartableComposable
    @Composable
    override fun bar() {}
}
