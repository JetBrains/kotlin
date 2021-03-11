// PROBLEM: none
class Foo {
    fun test() {
        fun <caret>equals(other: Foo?): Boolean {
            return true
        }
        equals(null)
    }
}
