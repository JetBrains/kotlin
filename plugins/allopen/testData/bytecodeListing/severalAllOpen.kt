annotation class AllOpen
annotation class AllOpen2

@AllOpen
@AllOpen2
class Test {
    val prop: String = ""
    fun method() {}
}