@file:OptIn(
  InternalComposeApi::class,
)
package test

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable


interface A {
    fun b() {}
}
class C {
    val foo = 1
    inner class D : A {
        override fun b() {
            print(foo)
        }
    }
}
