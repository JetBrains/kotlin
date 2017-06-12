// EXPECTED_REACHABLE_NODES: 493
/*
 * Issue: KT-4159 Kotlin to JS compiler crashes on code with ?: return
 *
 * Expression like "val s1 : String = s ?: return null" causes compiler to crash
 */

package foo

fun stringLen(s : String?) : Int {
    val s1 : String = s ?: return 0
    return s1.length
}

fun stringReturnInLeftLen(s : String?) : Int {
    val s1 = (if (s != null) { return s.length } else { null }) ?: return 0
}

fun box(): String {
    assertEquals(3, stringLen("box"))
    assertEquals(0, stringLen(""))
    assertEquals(0, stringLen(null))

    assertEquals(3, stringReturnInLeftLen("box"))
    assertEquals(0, stringReturnInLeftLen(""))
    assertEquals(0, stringReturnInLeftLen(null))

    return "OK"
}