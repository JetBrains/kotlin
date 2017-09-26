class TestAssignmentInCondition {
    private var i: Int = 0

    fun foo(x: Int) {
        i = x
        if (i > 0) println(">0")
    }
}