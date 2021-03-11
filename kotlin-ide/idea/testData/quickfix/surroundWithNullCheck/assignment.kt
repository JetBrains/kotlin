// "Surround with null check" "true"

fun foo(s: String?) {
    var ss: String = ""
    ss = <caret>s
}