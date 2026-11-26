// TARGET_BACKEND: NATIVE
// WITH_PLATFORM_LIBS
// LANGUAGE: +CollectionLiterals +CompanionBlocks

// The legacy lowering for *[...] uses arrayOf(...). KT-81722 switches it to Array.of(...)
// when the experimental CollectionLiterals feature is enabled. KT-82852 keeps variadic
// interop working with that lowering. The test enables the feature explicitly until it is on by default.

@file:Suppress("REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.ExperimentalCollectionLiteralsApi::class)

import kotlinx.cinterop.*
import platform.posix.snprintf

private fun checkFormatted(expected: String, format: (CPointer<ByteVar>) -> Int) = memScoped {
    val buffer = allocArray<ByteVar>(64)
    val length = format(buffer)
    check(length == expected.length) { "Expected length ${expected.length}, got $length" }
    val actual = buffer.toKString()
    check(actual == expected) { "Expected '$expected', got '$actual'" }
}

fun box(): String {
    checkFormatted("1:2:3") { snprintf(it, 64.convert(), "%d:%d:%d", *arrayOf(1, 2, 3)) }
    checkFormatted("1:2:3") { snprintf(it, 64.convert(), "%d:%d:%d", *Array.of(1, 2, 3)) }
    checkFormatted("1:2:3") { snprintf(it, 64.convert(), "%d:%d:%d", *[1, 2, 3]) }
    checkFormatted("1:2:3") { snprintf(it, 64.convert(), "%d:%d:%d", variadicArguments = *[1, 2, 3]) }
    checkFormatted("1:2:3") { snprintf(it, 64.convert(), "%d:%d:%d", variadicArguments = *Array.of(1, 2, 3)) }
    checkFormatted("1:2:3") { snprintf(it, 64.convert(), "%d:%d:%d", *arrayOf(1, *Array.of(2, 3))) }
    checkFormatted("1:2:3") { snprintf(it, 64.convert(), "%d:%d:%d", *Array.of(1, *arrayOf(2, 3))) }
    checkFormatted("1:2:3") { snprintf(it, 64.convert(), "%d:%d:%d", 1, *[2], 3) }
    checkFormatted("empty") { snprintf(it, 64.convert(), "empty", *[]) }
    checkFormatted("1:2.5") { snprintf(it, 64.convert(), "%d:%.1f", *[1, 2.5]) }
    return "OK"
}
