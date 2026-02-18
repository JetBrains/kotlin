package kt83736

class Foo

object Tests {
    fun testId(mapper: (id: Any, anotherId: Any) -> Unit) {}
    fun testCls(mapper: (Kt83736Foo: Any, foo: Foo) -> Unit) {}
    fun testOK(mapper: (id1: Any?, anotherId: Any?) -> Unit) {}
}
