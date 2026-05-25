@file:OptIn(
  InternalComposeApi::class,
)
package test

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable


    @Composable
    fun composeVector(
        composable: @Composable () -> Unit
    ) {
        emit {
            emit {
                composable()
            }
        }
    }
    @Composable
    inline fun emit(composable: @Composable () -> Unit) {
        composable()
    }
