package foo

class C: B()
open class B: A()
open class A

fun box(): String {
    val c = C()

    if (c !is A) return "FAIL"

    return "OK"
}