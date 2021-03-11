annotation class AllOpen

@AllOpen
class TestWithAllOpen {
    val prop: String = ""
    fun method() {}

    open val openProp: String = ""
    open fun openMethod() {}

    final val finalProp: String = ""
    final fun finalMethod() {}
}

class TestWithoutAllOpen {
    val prop: String = ""
    fun method() {}

    open val openProp: String = ""
    open fun openMethod() {}

    final val finalProp: String = ""
    final fun finalMethod() {}
}