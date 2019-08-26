var sideEffect = ""

abstract class A {
    abstract fun foo()
}

class B : A() {
    override fun foo() {
        sideEffect += "B"
    }
}

fun getB(): A {
    sideEffect += "A"
    return B()
}

fun box(): String {

    getB().foo()

    if (sideEffect != "AB")
         return "Fail: $sideEffect"

    return "OK"
}