class TestAssignmentInCondition {
    private var i = 0
    fun foo(x: Int) {
        if (x.also { i = it } > 0) println(">0")
    }
}