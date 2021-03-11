// "Initialize with constructor parameter" "true"
open class A {
    <caret>var n: Int
        get() = 1
}

class B : A()

fun test() {
    val a = A()
}