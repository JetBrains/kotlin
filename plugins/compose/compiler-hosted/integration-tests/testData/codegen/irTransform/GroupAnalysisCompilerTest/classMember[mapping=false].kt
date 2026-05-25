interface Test {
    @Composable
    fun Test()
}

class TestImpl : Test {
    @Composable
    override fun Test() {}
}

fun used(x: Any?) {}
