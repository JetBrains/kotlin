package foo

//KT-4054

class A() {
    fun f0() {
    }

    fun f1() {
        f0()
        this.f0()
    }
}

fun box(): String {
    return "OK"
}