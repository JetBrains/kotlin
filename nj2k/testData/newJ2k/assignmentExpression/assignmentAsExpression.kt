class AssignmentAsExpression {
    private var field = 0
    private var field2 = 0

    fun assign(value: Int) {
        field = value
        val v = field
        field2 = value
        field = field2
        val j: Int
        j = 0
        val i = j
    }
}