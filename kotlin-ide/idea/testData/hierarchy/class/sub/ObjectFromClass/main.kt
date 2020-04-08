open class <caret>A

object B: A()

class MyClass(a: A = run { object: A() {} }) {
    init {
        object C: A()
    }

    fun foo(a: A = run { object: A() {} }) {
        object D: A()
    }

    val t = object {
        object F: A()
    }
}

val bar: Int
    get() {
        object E: A()

        return 0
    }

val x = object: A() {

}