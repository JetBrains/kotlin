class TestClass<T>
private fun test()<caret> = {
    class Local
    TestClass<Local>()
}