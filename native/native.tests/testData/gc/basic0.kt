import kotlin.test.*

class A {
    var field: B? = null
}

class B(var field: Int)

@Test fun runTest() {
    val a = A()
    a.field = B(2)
}
