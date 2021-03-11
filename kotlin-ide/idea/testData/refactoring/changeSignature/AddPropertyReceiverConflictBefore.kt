open class A {
    open var <caret>p: Int = 1
}

fun test() {
    val t1 = A().p
    A().p = 1
}