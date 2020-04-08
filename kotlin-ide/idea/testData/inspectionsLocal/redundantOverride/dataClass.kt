// PROBLEM: none
data class My(val x: Int, val y: String) {
    override <caret>fun hashCode() = super.hashCode()
}