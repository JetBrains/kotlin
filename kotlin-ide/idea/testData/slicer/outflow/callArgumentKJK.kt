// FLOW: OUT
class K {
    fun bar(m: Int) {
        val z = m
    }

    fun test() {
        val x = <caret>1
        J().foo(x)
    }
}