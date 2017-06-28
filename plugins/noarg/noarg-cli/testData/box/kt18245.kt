// WITH_RUNTIME

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

    return "OK"
}