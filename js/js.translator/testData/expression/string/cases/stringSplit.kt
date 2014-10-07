package foo

fun box(): String {
    val testInput = "test data\t1foo  2 bar"
    val tests = array(
            " "     to array("test", "data\t1foo", "", "2", "bar"),
            "\\s+"  to array("test", "data", "1foo", "2", "bar"),
            "[sd]"  to array("te", "t ", "ata\t1foo  2 bar"),
            "[\\d]" to array("test data\t", "foo  ", " bar")
    )

    for (test in tests) {
        val regexp = test.first
        val expected = test.second
        val result = testInput.split(regexp)

        if (result != expected) return "Wrong result for '$regexp' -- Expected: $expected | Actual: $result"
    }

    for (test in tests) {
        val regexp = test.first
        val limit = 2
        val expected = Array(limit) { test.second[it] }
        val result = testInput.split(regexp, limit)

        if (result != expected) return "Wrong result for '$regexp' -- Expected: $expected | Actual: $result"
    }

    return "OK"
}