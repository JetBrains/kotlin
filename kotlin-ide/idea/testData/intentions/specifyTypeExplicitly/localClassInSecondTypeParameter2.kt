open class F
class TestClass<V, out K>

private fun test()<caret> = {
    class Local : F()
    TestClass<F, Local>()
}