// "Create type parameter 'T' in property 'a'" "true"
class Test {
    val T.a: <caret>T?
        get() = null
}