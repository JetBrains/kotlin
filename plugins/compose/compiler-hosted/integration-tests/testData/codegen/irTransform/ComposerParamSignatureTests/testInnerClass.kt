interface A {
    fun b() {}
}
class C {
    val foo = 1
    inner class D : A {
        override fun b() {
            print(foo)
        }
    }
}

fun used(x: Any?) {}
