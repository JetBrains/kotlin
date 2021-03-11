// "Remove variable 'a'" "true"
var cnt = 5
fun getCnt() = cnt++
fun f() {
    var <caret>a = getCnt()
}