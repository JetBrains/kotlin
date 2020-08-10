class TestClass {
    fun testClassFun() {}
}

<warning descr="SSR">val prop: TestClass.() -> Unit = {this.testClassFun()}</warning>

val bar: () -> Unit = {}