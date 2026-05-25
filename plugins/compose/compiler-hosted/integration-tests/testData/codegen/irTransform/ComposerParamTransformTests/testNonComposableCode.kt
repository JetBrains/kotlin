@file:OptIn(
  InternalComposeApi::class,
)
package test

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable


fun A() {}
val b: Int get() = 123
fun C(x: Int) {
    var x = 0
    x++

    class D {
        fun E() { A() }
        val F: Int get() = 123
    }
    val g = object { fun H() {} }
}
fun I(block: () -> Unit) { block() }
fun J() {
    I {
        I {
            A()
        }
    }
}
