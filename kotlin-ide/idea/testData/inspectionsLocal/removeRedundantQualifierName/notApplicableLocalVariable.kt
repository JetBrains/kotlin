// PROBLEM: none
package my.simple.name

class Inner {
    fun a() {
        val MAX = 2
        val a = Member<caret>.MAX
    }

    companion object Member {
        val MAX = 1
    }
}
