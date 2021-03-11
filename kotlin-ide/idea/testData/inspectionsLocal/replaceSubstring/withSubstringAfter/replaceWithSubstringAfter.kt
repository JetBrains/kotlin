// WITH_RUNTIME

fun foo(s: String) {
    s.substring<caret>(s.indexOf('x'))
}