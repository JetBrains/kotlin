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
