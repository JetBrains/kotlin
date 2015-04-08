package foo

open class A() {

    var order = ""
    init {
        order = order + "A"
    }
}

open class B() : A() {
    init {
        order = order + "B"
    }

}

class C() : B() {
    init {
        order = order + "C"
    }
}

fun box(): Boolean {
    return (C().order == "ABC") && (B().order == "AB") && (A().order == "A")
}