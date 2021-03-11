class Test {
    inner class Inner {
        fun bar() {
            listOf("").forEach {
                <caret>foo(it)
            }
        }

        fun foo(v: String) {
            bizz(v)
        }
    }

    fun bizz(v:String) {
        v.toString()
    }
}