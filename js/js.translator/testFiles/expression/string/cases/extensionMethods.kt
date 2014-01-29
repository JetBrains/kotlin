package foo

val testString = "foobarbaz"
val testStringSize = 9
val emptyString = ""
val startsWithParam = "foo"
val endsWithParam = "az"
val containsParam = "ar"

fun assertEquals(actual: Any, expected: Any, s: String, whatTested: String) =
        if (expected != actual) "String.$whatTested fails on \"$s\", expected: $expected, actual: $actual" else null

fun assertEquals(actual: Any, expected: Any, s: CharSequence, whatTested: CharSequence) =
        if (expected != actual) "CharSequence.$whatTested fails on \"$s\", expected: $expected, actual: $actual" else null

fun testString(s: String, expectedSize: Int): String? =
        assertEquals(s.size, expectedSize, s, "size") ?:
        assertEquals(s.length(), expectedSize, s, "length()") ?:
        assertEquals(s.length, expectedSize, s, "length") ?:
        assertEquals(s.isEmpty(), expectedSize == 0, s, "isEmpty()") ?:
        assertEquals(s.startsWith(startsWithParam), expectedSize != 0, s, "startsWith(\"$startsWithParam\")") ?:
        assertEquals(s.endsWith(endsWithParam), expectedSize != 0, s, "endsWith(\"$endsWithParam\")") ?:
        assertEquals(s.contains(containsParam), expectedSize != 0, s, "contains(\"$containsParam\")")

fun testCharSequence(s: CharSequence, expectedSize: Int): String? =
        assertEquals(s.size, expectedSize, s, "size") ?:
        assertEquals(s.length(), expectedSize, s, "length()") ?:
        assertEquals(s.length, expectedSize, s, "length") ?:
        assertEquals(s.isEmpty(), expectedSize == 0, s, "isEmpty()")

fun box(): String =
        testString(testString, testStringSize) ?:
        testString(emptyString, 0) ?:
        testCharSequence(testString, testStringSize) ?:
        testCharSequence(emptyString, 0) ?:
        "OK"
