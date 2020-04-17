class A {
    inner class B {
        fun foo() {
            println(<warning descr="SSR">this@A</warning>)
            println(this@B)
        }
    }
}