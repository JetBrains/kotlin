// PROBLEM: none
fun test(i: Int?): Int {
    val x = i!!<caret>
    return x
}