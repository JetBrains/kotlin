// PROBLEM: none
package my.simple.name

class Inner {
    fun a() {
        val a = Member<caret>.MAX
    }

    companion object Member {
        val MAX = 1
    }
}
