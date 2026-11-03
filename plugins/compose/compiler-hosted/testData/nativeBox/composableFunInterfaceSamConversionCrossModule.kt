// FREE_COMPILER_ARGS: -Xverify-ir=none
// REASON: Compose runtime klib used in tests is compiled with Kotlin 2.3.20, whose Compose plugin
// generated ComposableSingletons objects without supertypes (fixed in 2.5.0 by KT-87962). IR validation
// of the deserialized runtime fails with "IrClass must have at least one supertype". Remove once tests
// use a Compose runtime compiled with Kotlin 2.5.0 or newer.

// MODULE: lib
// FILE: lib.kt
package lib

import androidx.compose.runtime.*

fun interface Decorator {
    @Composable
    fun Decoration(inner: @Composable () -> Unit)
}

// MODULE: main(lib)
// FILE: main.kt
import androidx.compose.runtime.*
import lib.*

fun decorator(): Decorator = Decorator { inner -> inner() }

fun box(): String {
    decorator()
    return "OK"
}
