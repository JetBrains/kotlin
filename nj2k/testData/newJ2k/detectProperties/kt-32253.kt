class TestInitInCtor(private val i: Int) {
    private val j: Int
    fun foo(): Int {
        return i + j
    }

    init {
        j = i
    }
}
