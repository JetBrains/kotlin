// PROBLEM: none
fun test(n: Int): String {
    var res: String = ""
    var res2: String = ""

    <caret>if (n == 1) {
        res = "one"
    } else {
        res2 = "two"
    }

    return res + res2
}