// PROBLEM: none
class F {
    companion object {
        fun <caret>equals(other: F?): Boolean {
            return true
        }
    }
}