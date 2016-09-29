annotation class AllOpen

@AllOpen
open class Test1

@AllOpen
open class Test2 {
    open fun method() {}
    val prop: String = ""
}

@AllOpen
class Test3 {
    fun method() {}
    open val prop: String = ""
}