package foo

inline fun assertRegex(regexFactory: () -> dynamic, expectedPattern: String, expectedFlags: String, testString: String) {
    val reg = regexFactory()
    assertEquals(expectedPattern, reg.source)
    assertEquals(expectedFlags, reg.flags)
    assertEquals(true, reg.test(testString))
}

fun box(): String {
    assertRegex({ js("/ab+c/u") }, "ab+c", "u", "abbbbc")
    assertRegex({ js("/^ +$/") }, "^ +$", "", "   ")
    assertRegex({ js("/\\u{1F600}/u") }, "\\u{1F600}", "u", "\uD83D\uDE00")

    return "OK"
}
