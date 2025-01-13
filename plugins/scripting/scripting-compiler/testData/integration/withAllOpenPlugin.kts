annotation class AllOpen

@AllOpen
class Test {
    val prop: String = "OK"
}

class A : Test()

A().prop
