// WITH_STDLIB
// SKIP_IR_DESERIALIZATION_CHECKS
// REASON: the ComposableSingletons class the plugin generates has no fake overrides for kotlin.Any
// members, while the fake override builder synthesizes them during KLIB linking.

// The type parameter sits inside an invariant type argument: `MutableList<E>`.
// `MutableList<S>` is not a subtype of `MutableList<Any?>`, so the erased
// `Function3<MutableList<Any?>, ...>` is not a subtype of `Function3<MutableList<S>, ...>` either.
//
// The dump therefore shows a TYPE_OP ... origin=IMPLICIT_CAST around the read from
// ComposableSingletons. That is the IR counterpart of an unchecked cast in the source language: no
// runtime check is emitted, and none is possible, because type arguments are erased.

import androidx.compose.runtime.*

@Composable
fun <S> Test(vm: MutableList<S>, content: @Composable (MutableList<S>) -> Unit = { Effect(it) }) {
    content(vm)
}

@Composable
fun <S> Effect(vm: MutableList<S>) {
    println(vm)
}
