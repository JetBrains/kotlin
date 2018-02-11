package test

import kotlin.internal.NoInfer
import kotlin.test.fail

// just a static type check
fun <T> assertStaticTypeIs(@Suppress("UNUSED_PARAMETER") value: @NoInfer T) {}

inline fun <reified T> assertStaticAndRuntimeTypeIs(value: @NoInfer T) {
    @Suppress("USELESS_CAST")
    if ((value as Any?) !is T) {
        fail("Expected value $value to have ${T::class} type")
    }
}