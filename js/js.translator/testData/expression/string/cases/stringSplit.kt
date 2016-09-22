package foo

fun box(): String {
    val testInput = "test data\t1foo  2 bar"
    val tests = arrayOf(
            " "     to arrayOf("test", "data\t1foo", "", "2", "bar"),
            "\\s+"  to arrayOf("test", "data", "1foo", "2", "bar"),
            "[sd]"  to arrayOf("te", "t ", "ata\t1foo  2 bar"),
            "[\\d]" to arrayOf("test data\t", "foo  ", " bar")
    )

    for (test in tests) {
        val regexp = test.first
        val expected = test.second
        val result = testInput.splitWithRegex(regexp)

        if (result.asList() != expected.asList()) return "Wrong result for '$regexp' -- Expected: $expected | Actual: $result"
    }

    for (test in tests) {
        val regexp = test.first
        val limit = 2
        val expected = Array(limit) { test.second[it] }
        val result = testInput.splitWithRegex(regexp, limit)

        if (result.asList() != expected.asList()) return "Wrong result for '$regexp' -- Expected: $expected | Actual: $result"
    }

    return "OK"
}