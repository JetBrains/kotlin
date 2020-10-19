class A {
    val x = 1

    fun foo() {
        print(<warning descr="SSR">this::x</warning>)
    }
}

class B {
    val x = 1

    fun foo() {
        print(this::x)
    }
}