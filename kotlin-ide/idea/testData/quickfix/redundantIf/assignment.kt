// "Remove redundant 'if' statement" "true"
fun bar(p: Int) {
    var v2 = false
    <caret>if (p > 0) {
        v2 = false
    }
    else {
        v2 = true
    }
}