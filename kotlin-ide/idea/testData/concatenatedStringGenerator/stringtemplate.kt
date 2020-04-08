// WITH_RUNTIME

var foo = "aaa"
var bar = "bbb"
fun bar2() = "will be ignored"

val test = "foo$foo$bar" + "${bar}${bar2()}bar"