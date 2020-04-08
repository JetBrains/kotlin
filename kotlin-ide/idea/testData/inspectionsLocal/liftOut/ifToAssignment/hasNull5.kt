// PROBLEM: none
// DISABLE-ERRORS
fun bar(i: Int) {
    var str: String = ""

    <caret>if (i == 1) {
        str = null
    } else if (i == 2) {
        str = "2"
    } else {
        str = "3"
    }
}