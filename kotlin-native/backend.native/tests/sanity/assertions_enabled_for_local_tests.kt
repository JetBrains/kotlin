@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)
package sanity.assertions_enabled

import kotlin.test.Test

// Just to make sure that assertions are enabled for `KonanLocalTest`s.
@Test fun runTest() {
    main(emptyArray<String>())
}

fun main(args: Array<String>) {
    try {
        assert(false) // Should throw AssertionError.
        throw Error("Assertions are disabled. Please make sure the tests were compiled with '-ea' option.") // Normally unreachable line.
    } catch (e: AssertionError) {
        // That's OK.
    }
}
