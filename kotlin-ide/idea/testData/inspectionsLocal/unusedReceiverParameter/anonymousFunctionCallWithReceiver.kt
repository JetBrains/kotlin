// PROBLEM: none
class A {
    fun a() {
        this.(fun <caret>A.() {
        })()
    }
}