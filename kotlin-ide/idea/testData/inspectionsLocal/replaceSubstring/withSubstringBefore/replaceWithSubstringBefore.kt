// WITH_RUNTIME

fun foo(s: String) {
    s.substring<caret>(0, s.indexOf('x'))
}