// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

open class Base {
    open fun greet(name: String): String = "Hello, $name"
    open fun count(): Int = 42
    fun notOpen(): String = "final"
}

abstract class AbstractBase {
    abstract fun abstractMethod(): String
    open fun concreteMethod(): Int = 0
}

interface Greeter {
    fun greet(name: String): String
    fun salutation(): String
}

open class GreeterBase : Greeter {
    override fun greet(name: String): String = "Hello, $name"
    override fun salutation(): String = "Hi"
}
