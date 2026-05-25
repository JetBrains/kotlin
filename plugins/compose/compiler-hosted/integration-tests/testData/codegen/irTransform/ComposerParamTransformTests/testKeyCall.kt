@file:OptIn(
  InternalComposeApi::class,
)
package test

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable


    import androidx.compose.runtime.key

    @Composable
    fun Wrapper(block: @Composable () -> Unit) {
        block()
    }

    @Composable
    fun Leaf(text: String) {
        used(text)
    }

    @Composable
    fun Test(value: Int) {
        key(value) {
            Wrapper {
                Leaf("Value $value")
            }
        }
    }
