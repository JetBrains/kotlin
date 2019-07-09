// DONT_TARGET_EXACT_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1241

// Not targeting JS_IR because it's too implementaion-specific
// for legacy JS backend

package foo

fun assertContains(expectedName: String, f: () -> Unit) {
    val s = f.toString()
    assertTrue(s.contains(expectedName), "\"$s\" dosn't contain \"$expectedName\"")
}

fun box(): String {
    val d: dynamic = bar

    val a = {
        val somethingBefore = 1
        d.somethingBefore
    }

    assertContains("var somethingBefore = 1;", a)

    val b = {
        d.somethingAfter
        val somethingAfter = 1
    }

    assertContains("var somethingAfter = 1;", b)

    return "OK"
}