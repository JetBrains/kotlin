// FREE_COMPILER_ARGS: -Xverify-ir=none
// REASON: Compose runtime klib used in tests is compiled with Kotlin 2.3.20, whose Compose plugin
// generated ComposableSingletons objects without supertypes (fixed in 2.5.0 by KT-87962). IR validation
// of the deserialized runtime fails with "IrClass must have at least one supertype". Remove once tests
// use a Compose runtime compiled with Kotlin 2.5.0 or newer.

import androidx.compose.runtime.*

@Composable
fun Content(value: Int) {
    remember(value) { value }
}

val content: @Composable () -> Unit = { Content(42) }

fun box(): String {
    content
    return "OK"
}
