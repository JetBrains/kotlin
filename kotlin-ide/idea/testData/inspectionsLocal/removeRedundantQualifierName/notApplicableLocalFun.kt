// PROBLEM: none
package my.simple.name

class Inner {
    fun a() {
        fun say() {}
        Inner<caret>.say()
    }

    companion object {
        fun say() {}
    }
}
