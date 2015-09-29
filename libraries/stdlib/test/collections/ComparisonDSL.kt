package test.collections

import kotlin.test.*

public fun compare<T>(expected: T, actual: T, block:CompareContext<T>.() -> Unit) {
    CompareContext(expected, actual).block()
}

public class CompareContext<out T>(public val expected: T, public val actual: T) {

    public fun equals(message: String = "") {
        assertEquals(expected, actual, message)
    }
    public fun propertyEquals<P>(message: String = "", getter: T.() -> P) {
        assertEquals(expected.getter(), actual.getter(), message)
    }
    public fun propertyFails(getter: T.() -> Unit) { assertFailEquals({expected.getter()}, {actual.getter()}) }
    public fun compareProperty<P>(getter: T.() -> P, block: CompareContext<P>.() -> Unit) {
        compare(expected.getter(), actual.getter(), block)
    }

    private fun assertFailEquals(expected: () -> Unit, actual: () -> Unit) {
        val expectedFail = assertFails(expected)
        val actualFail = assertFails(actual)
        //assertEquals(expectedFail != null, actualFail != null)
        assertTypeEquals(expectedFail, actualFail)
    }
}