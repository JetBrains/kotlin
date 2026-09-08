// WITH_STDLIB
// SKIP_IR_DESERIALIZATION_CHECKS
// REASON: the ComposableSingletons class the plugin generates has no fake overrides for kotlin.Any
// members, while the fake override builder synthesizes them during KLIB linking.

// Same invariant type argument as in composableSingletonInvariantArgument.kt, but the lambda sits in
// a public inline function. Public inline functions must not read the singleton instance — changes to
// the function body would be ABI breaking — so a fresh `composableLambdaInstance` is built instead,
// and the singleton is only kept around as a stub for existing consumers.
//
// The type parameters of the enclosing declaration are erased for the singleton's sake, since
// ComposableSingletons is a non-generic object. That erasure must not leak into the inline copy: it
// stays in the generic scope, and its lambda still takes `MutableList<S>`. The IMPLICIT_CAST around
// `composableLambdaInstance` therefore has to name the non-erased `Function3<MutableList<S>, ...>`,
// while the one around the singleton read in `Plain` names the same type for the erased read.

import androidx.compose.runtime.*

@Composable
fun <S> Consume(content: @Composable (MutableList<S>) -> Unit) {
    println(content)
}

@Composable
inline fun <S> PublicInline() {
    Consume<S> { }
}

@Composable
fun <S> Plain() {
    Consume<S> { }
}
