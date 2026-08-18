// KIND: STANDALONE
// KT-87165: boxes identity: `===` and `identityHashCode`.
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.4
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.native.identityHashCode

fun box(): String {
    val one: Any = 1_000_000
    val sameOne: Any = one
    val hash1 = one.identityHashCode()
    val hash2 = sameOne.identityHashCode()
    val referentiallyEqual = (one === sameOne)

    if (!referentiallyEqual) return "fail: === is false"
    if (hash1 != hash2) return "fail: identityHashCode are different"

    return "OK"
}
