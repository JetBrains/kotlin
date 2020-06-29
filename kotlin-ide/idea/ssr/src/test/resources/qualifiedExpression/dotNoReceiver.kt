class MyClass {
    fun foo() {}

    fun secondClassTestFun() {
        <warning descr="SSR">foo()</warning>
        this.foo()

        MyClass().foo()

        val myClass = <warning descr="SSR">MyClass()</warning>
        myClass.foo()
    }
}