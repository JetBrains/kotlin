class FirstClass {

    fun firstClassFun() {}
    fun firstClassFunTwo(i: Int) { <warning descr="SSR">print(i)</warning> }

    fun testFoo() {
        <warning descr="SSR">firstClassFun()</warning>
        <warning descr="SSR">firstClassFunTwo(2)</warning>
    }

}