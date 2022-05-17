// WITH_STDLIB

annotation class NoArg

sealed class Test {
    abstract val test: String

    @NoArg
    data class Test1(override val test: String) : Test()

    @NoArg
    data class Test2(override val test: String) : Test()
}

fun box(): String {
    Test::class.java.declaredConstructors.forEach { it.isAccessible = true }
    Test.Test1::class.java.declaredConstructors.forEach { it.isAccessible = true }

    val instance = Test.Test1::class.java.newInstance() // Error

    Demo.Foo::class.java.newInstance()
    Demo.Free::class.java.newInstance()

    A.Free::class.java.newInstance()

    return "OK"
}

@NoArg
sealed class Demo(val name : String) {
    @NoArg
    class Free(name: String)

    @NoArg
    class Foo(name: String) : Demo(name)

    @NoArg
    class Bar(name: String) : Demo(name)
}

@NoArg
abstract class A(val name: String) {
    @NoArg
    class Free(name: String) : A(name)
}