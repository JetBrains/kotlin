// PROBLEM: none
class A {
    fun a() {}

    <caret>inner class B {
        fun b() {
            this@A
        }
    }
}