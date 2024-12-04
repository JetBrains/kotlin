annotation class AllOpen

@AllOpen
final class Test1

@AllOpen
class Test2 {
    fun method1() {}
    val prop1: String = ""

    final fun method2() {}
    final val prop2: String = ""
    final var prop3: String = ""

    // Modifier 'final' is not applicable to 'setter'
    // var prop4: String = ""
    //     final set
}