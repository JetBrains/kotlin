class C {
    companion object {
        fun create() = C()
    }
    fun test() {
        <caret>Companion.create()
    }
}