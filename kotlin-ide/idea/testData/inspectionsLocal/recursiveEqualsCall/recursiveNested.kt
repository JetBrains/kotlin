open class Outer {
    inner class Nested : Outer() {
        override fun equals(other: Any?): Boolean {
            if (this@Nested <caret>== other) return true
            return false
        }
    }
}