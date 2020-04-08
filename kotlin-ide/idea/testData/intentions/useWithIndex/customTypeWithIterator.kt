// IS_APPLICABLE: false
// WITH_RUNTIME
class X {
    operator fun iterator(): Iterator<String>{
        return emptyList<String>().iterator()
    }
}

fun foo(x: X) {
    var index = 0
    <caret>for (s in x) {
        if (s.length > index++) {
            println(s)
        }
    }
}
