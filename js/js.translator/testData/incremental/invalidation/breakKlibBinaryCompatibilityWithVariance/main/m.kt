fun box(stepId: Int): String {
    var x = -1
    TestClass().test {
        object : GenericInterface<Int> {
            init {
                x = stepId
            }
        }
    }

    when (stepId) {
        in 0..1 -> if (x != stepId) return "Fail, got $x"
        else -> return "Unknown"
    }
    return "OK"
}
