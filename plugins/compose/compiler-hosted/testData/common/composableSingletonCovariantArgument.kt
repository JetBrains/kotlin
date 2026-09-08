// WITH_STDLIB
// SKIP_IR_DESERIALIZATION_CHECKS
// REASON: the ComposableSingletons class the plugin generates has no fake overrides for kotlin.Any
// members, while the fake override builder synthesizes them during KLIB linking.

// The type parameter sits inside a covariant type argument: `List<out E>`.
// Erasure gives `List<Any?>`, and `List<S>` is a subtype of `List<Any?>`, which combined with the
// contravariance of the function parameter makes `Function3<List<Any?>, ...>` a subtype of
// `Function3<List<S>, ...>`. The singleton is therefore read without a cast, same as the plain
// type parameter case.

import androidx.compose.runtime.*

@Composable
fun <S> Test(vm: List<S>, content: @Composable (List<S>) -> Unit = { Effect(it) }) {
    content(vm)
}

@Composable
fun <S> Effect(vm: List<S>) {
    println(vm)
}
