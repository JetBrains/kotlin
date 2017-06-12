// EXPECTED_REACHABLE_NODES: 492
/*
 * Issue: KT-4159 Kotlin to JS compiler crashes on code with ?: return
 *
 * Expression like "val s1 : String = s ?: return null" causes compiler to crash
 */

package foo

fun firstNotNullLen(s1 : String?, s2 : String?, s3 : String?) : Int {
    val len = (s1?.length ?: s2?.length) ?:
                (s2?.length ?: s3?.length) ?:
                    return 0
    return len
}

fun box(): String {
    assertEquals(1, firstNotNullLen("a", null, null))
    assertEquals(2, firstNotNullLen(null, "ab", null))
    assertEquals(3, firstNotNullLen(null, null, "abc"))
    assertEquals(0, firstNotNullLen(null, null, null))

    return "OK"
}