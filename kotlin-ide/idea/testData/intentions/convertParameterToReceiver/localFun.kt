fun test() {
    fun foo(<caret>s: String, n: Int): Boolean {
        return s.length - n/2 > 1
    }

    foo("1", 2)
}