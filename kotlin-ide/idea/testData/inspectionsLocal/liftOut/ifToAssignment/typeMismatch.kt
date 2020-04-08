// PROBLEM: none
fun test(b: Boolean, x: Long, y: Int) {
    var num: Long = 0L
    <caret>if (b) {
        num += x
    } else {
        num += y
    }
}