package foo

fun testRenamed(case: String, f: () -> Unit) = test(case, true, f)
fun testNotRenamed(case: String, f: () -> Unit) = test(case, false, f)

native
fun String.replace(r: String, s: String): String = noImpl

native
class RegExp(pattern: String, flag: String)

native
fun String.match(r: RegExp): Array<String> = noImpl

fun test(keyword: String, expectedRenamed: Boolean, f: Any) {
    val fs = f.toString().replace("while (false)", "")
    val matches = fs.match(RegExp("[\\w$]*$keyword[\\w_$]*", "g"))

    assertNotEquals(null, matches, "matches == null, fs = $fs")
    assertNotEquals(0, matches.size, "matches = $matches")

    val actual = matches[matches.size -1]

    assertTrue(actual.contains(keyword), "'$keyword' not found in '$matches' from '$fs'")

    if (expectedRenamed) {
        assertNotEquals(keyword, actual, "fs = $fs")
    }
    else {
        assertEquals(keyword, actual, "fs = $fs")
    }
}
