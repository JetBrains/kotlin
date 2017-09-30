class TestAssignmentInArgumentConfusingResolve {
    private var x = 0

    fun setX(xx: Int) {
        x = xx
        notify(x)
    }

    private fun notify(x: Int) {}
}