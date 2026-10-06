// KIND: STANDALONE
// MODULE: VarargInInterface
// FILE: main.kt
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative

interface Driver {
    fun addListener(vararg queryKeys: String): String
}

interface Counter {
    fun count(vararg values: Int): Int
}

interface OverloadedCounter {
    fun add(vararg values: Int): Int
    fun add(value: Int): Int
}

open class KotlinObject

fun useDriver(driver: Driver): String = driver.addListener("a", "b", "c")

fun useCounter(counter: Counter): Int = counter.count(1, 2, 3)

fun callVarargAdd(counter: OverloadedCounter): Int = counter.add(1, 2, 3)
fun callSingleAdd(counter: OverloadedCounter): Int = counter.add(10)

open class BaseDriver {
    open fun addListener(vararg queryKeys: String): String = "base:" + queryKeys.joinToString(",")
}

fun useBase(base: BaseDriver): String = base.addListener("x", "y")
