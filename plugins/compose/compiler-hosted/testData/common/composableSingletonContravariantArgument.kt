// WITH_STDLIB
// SKIP_IR_DESERIALIZATION_CHECKS
// REASON: the ComposableSingletons class the plugin generates has no fake overrides for kotlin.Any
// members, while the fake override builder synthesizes them during KLIB linking.

// The type parameter sits inside a contravariant type argument: `Comparator<in T>`.
// Erasure gives `Comparator<Any?>`, and contravariance flips the direction, so `Comparator<S>` is
// not a subtype of `Comparator<Any?>` - only the other way round. Just like the invariant case, the
// erased function type is not a subtype of the original one, so the read from ComposableSingletons
// is wrapped in an IMPLICIT_CAST.

import androidx.compose.runtime.*

@Composable
fun <S> Test(vm: Comparator<S>, content: @Composable (Comparator<S>) -> Unit = { Effect(it) }) {
    content(vm)
}

@Composable
fun <S> Effect(vm: Comparator<S>) {
    println(vm)
}
