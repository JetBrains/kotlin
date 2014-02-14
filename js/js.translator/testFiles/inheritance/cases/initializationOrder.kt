package foo

open class A() {

    var order = ""
    {
        order = order + "A"
    }
}

open class B() : A() {
    {
        order = order + "B"
    }

}

class C() : B() {
    {
        order = order + "C"
    }
}

fun box(): Boolean {
    return (C().order == "ABC") && (B().order == "AB") && (A().order == "A")
}