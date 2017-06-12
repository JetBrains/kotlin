// EXPECTED_REACHABLE_NODES: 551
package foo

import kotlin.reflect.KClass

inline fun <reified T : Any> foo(b: Boolean = false): () -> KClass<T> {
    if (b) {
        val T = 1
    }
    return { T::class }
}

fun box(): String {
    check(A::class, foo<A>()())
    check(B::class, foo<B>()())
    check(O::class, foo<O>()())
    check(E::class, foo<E>()())

    return "OK"
}
