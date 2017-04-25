// EXPECTED_REACHABLE_NODES: 501
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

fun box(): String {
    return if ((C().order == "ABC") && (B().order == "AB") && (A().order == "A")) "OK" else "fail"
}