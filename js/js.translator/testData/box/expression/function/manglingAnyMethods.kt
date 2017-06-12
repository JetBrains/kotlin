// EXPECTED_REACHABLE_NODES: 540
package foo

public class PublicClass {
    override fun equals(a: Any?): Boolean = this === a
    override fun hashCode(): Int = 0
    override fun toString(): String = "PublicClass"
}

internal class InternalClass {
    override fun equals(a: Any?): Boolean = this === a
    override fun hashCode(): Int = 1
    override fun toString(): String = "InternalClass"

    // overloads
    public fun equals(a: Any?, b: Any?): Boolean = a == b
    public fun hashCode(i: Int): Int = i
    public fun toString(s: String): String = s
}

private class PrivateClass {
    override fun equals(a: Any?): Boolean = this === a
    override fun hashCode(): Int = 2
    override fun toString(): String = "InternalClass"

    // overloads
    public fun equals(a: Any?, b: Any?): Boolean = a == b
    public fun hashCode(i: Int): Int = i
    public fun toString(s: String): String = s
}

// Helpers

val CALEE_NAME = RegExp("""((?:equals|hashCode|toString)[^(]*)""")

fun <T> Function0<T>.extractNames(): Array<String> {
    val names = CALEE_NAME.exec(this.toString())

    if (names == null || names.size != 2) {
        throw Exception("Cannot extract function name, $names for actual = \"$this\"")
    }

    return names
}

// Testing

var testGroup = ""

fun test(expected: String, f: () -> Unit) {
    val actual = f.extractNames()

    if (expected != actual[1]) {
        fail("Failed on '$testGroup' group: expected = \"$expected\", actual[1] = \"${actual[1]}\"\n actual = $actual")
    }
}

val STABLE_EQUALS = "equals"
val STABLE_HASH_CODE = "hashCode"
val STABLE_TO_STRING = "toString"
val STABLE_EQUALS_2 = "equals_oaftn8$"
val STABLE_HASH_CODE_2 = "hashCode_za3lpa$"
val STABLE_TO_STRING_2 = "toString_61zpoe$"

fun box(): String {
    testGroup = "Public Class"
    test(STABLE_EQUALS) { PublicClass().equals(0) }
    test(STABLE_HASH_CODE) { PublicClass().hashCode() }
    test(STABLE_TO_STRING) { PublicClass().toString() }

    testGroup = "Internal Class"
    test(STABLE_EQUALS) { InternalClass().equals(0) }
    test(STABLE_HASH_CODE) { InternalClass().hashCode() }
    test(STABLE_TO_STRING) { InternalClass().toString() }
    test(STABLE_EQUALS_2) { InternalClass().equals(0, 1) }
    test(STABLE_HASH_CODE_2) { InternalClass().hashCode(2) }
    test(STABLE_TO_STRING_2) { InternalClass().toString("3") }

    testGroup = "Private Class"
    test(STABLE_EQUALS) { PrivateClass().equals(0) }
    test(STABLE_HASH_CODE) { PrivateClass().hashCode() }
    test(STABLE_TO_STRING) { PrivateClass().toString() }
    test(STABLE_EQUALS_2) { PrivateClass().equals(0, 1) }
    test(STABLE_HASH_CODE_2) { PrivateClass().hashCode(2) }
    test(STABLE_TO_STRING_2) { PrivateClass().toString("3") }

    return "OK"
}
