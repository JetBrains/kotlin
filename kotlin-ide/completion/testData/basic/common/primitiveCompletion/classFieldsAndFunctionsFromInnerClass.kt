// FIR_COMPARISON
class A {
    fun aa() {}
    val aaa = 10

    inner class AA {
        fun bb() {}
        val bbb = 20

        fun test() {
            <caret>
        }
    }
}

// EXIST: aa
// EXIST: aaa
// EXIST: bb
// EXIST: bbb