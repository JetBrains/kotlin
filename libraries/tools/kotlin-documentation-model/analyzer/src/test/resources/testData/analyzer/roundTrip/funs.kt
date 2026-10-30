// MODULE: main
// FILE: example/funs.kt
package example

interface BaseFunctions {
    fun execute(): Unit
}

external fun externalCall(value: String): String

tailrec fun factorial(value: Int, accumulator: Int = 1): Int

open class FunctionOverrides : BaseFunctions {
    final override fun execute(): Unit

    open fun openCall(): Unit
}

abstract class Functions {
    abstract fun collect(vararg values: String): List<String>

    abstract fun defaults(name: String = "default", count: Int = name.length): String

    abstract fun <T : CharSequence> generic(value: T): T

    abstract operator fun get(index: Int): String

    abstract infix fun merge(other: Functions): Functions

    abstract fun overloaded(value: Int): String

    abstract fun overloaded(value: String): String

    abstract fun unit(): Unit

    abstract suspend fun update(value: String): Boolean
}

inline fun <reified T> identity(value: T): T

inline fun invoke(crossinline action: () -> Unit, noinline fallback: () -> Unit): Unit
