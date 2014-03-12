/*
 * Issue: KT-4159 Kotlin to JS compiler crashes on code with ?: return
 *
 * Expression like "val s1 : String = s ?: return null"
 * causes compiler to crash
 */

package foo

fun assertEquals<T>(expected: T, actual: T) {
    if (expected != actual) throw Exception("expected: $expected, actual: $actual")
}

fun firstNotNullLen(s1 : String?, s2 : String?, s3 : String?) : Int {
    val len = (s1?.length() ?: s2?.length()) ?:
                (s2?.length() ?: s3?.length()) ?:
                    return 0
    return len
}

fun nestedElvisReturnTest() {
    assertEquals(1, firstNotNullLen("a", null, null))
    assertEquals(2, firstNotNullLen(null, "ab", null))
    assertEquals(3, firstNotNullLen(null, null, "abc"))
    assertEquals(0, firstNotNullLen(null, null, null))
}

fun box(): String {
    nestedElvisReturnTest()

    return "OK"
}