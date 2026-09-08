// WITH_STDLIB
// SKIP_IR_DESERIALIZATION_CHECKS
// REASON: the ComposableSingletons class the plugin generates has no fake overrides for kotlin.Any
// members, while the fake override builder synthesizes them during KLIB linking.

// The lambda type mentions the enclosing function's type parameter directly: `@Composable (S) -> Unit`.
// This is the shape that used to make the singleton capture the caller's `$dirty`.
//
// Two positions to watch in the dump:
//   * the lambda declaration carries its own copy of `S`, so its body is self contained;
//   * the property and field holding the singleton must not mention `S` at all, since
//     ComposableSingletons is a non-generic object shared by every instantiation. Note also that the
//     backing field is nullable while the getter returns a non-null type.
//
// `Function3` is contravariant in its parameters, so the erased `Function3<Any?, ...>` is a subtype
// of `Function3<S, ...>` and the use site needs no cast.

import androidx.compose.runtime.*

@Composable
fun <S> Test(vm: S, content: @Composable (S) -> Unit = { Effect(it) }) {
    content(vm)
}

@Composable
fun <S> Effect(vm: S) {
    println(vm)
}
