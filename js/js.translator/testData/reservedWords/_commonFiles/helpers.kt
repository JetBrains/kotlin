package foo

import kotlin.text.Regex

fun testRenamed(case: String, f: () -> Unit) = test(case, true, f)
fun testNotRenamed(case: String, f: () -> Unit) = test(case, false, f)

fun test(keyword: String, expectedRenamed: Boolean, f: Any) {
    val fs = f.toString().replace("while (false)", "")
    val matches = Regex("[\\w$]*$keyword[\\w_$]*").matchAll(fs).map { it.value }.toList()

    assertNotEquals(0, matches.size(), "matches is empty for fs = $fs")

    val actual = matches.last()

    assertTrue(actual.contains(keyword), "'$keyword' not found in '$matches' from '$fs'")

    if (expectedRenamed) {
        assertNotEquals(keyword, actual, "fs = $fs")
    }
    else {
        assertEquals(keyword, actual, "fs = $fs")
    }
}
