annotation class AllOpen

@AllOpen
final class Test1

@AllOpen
class Test2 {
    fun method1() {}
    val prop1: String = ""

    final fun method2() {}
    val prop2: String = ""
}