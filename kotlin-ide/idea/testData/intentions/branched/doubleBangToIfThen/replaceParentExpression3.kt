// WITH_RUNTIME
fun foo(): String? {
    return "foo"
}

class A {
    fun f(): Int {
        return 42
    }
}

fun main(args: Array<String>) {
    val a: A? = A()
    val b = a<caret>!!.f()
}
