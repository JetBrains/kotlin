// KIND: STANDALONE
// FREE_COMPILER_ARGS: -Xreturn-value-checker=check
// MODULE: DiscardableResult
// FILE: discardable_result.kt

var calls: Int = 0

fun discardable(): Int = ++calls

@MustUseReturnValues
interface Greeter {
    fun greet(name: String): String
}

object SilentGreeter : Greeter {
    @IgnorableReturnValue
    override fun greet(name: String): String {
        calls++
        return ""
    }
}

class LoudGreeter : Greeter {
    override fun greet(name: String): String {
        calls++
        return "HELLO, $name"
    }
}

class Counter {
    var value: Int = 0

    @IgnorableReturnValue
    fun increment(): Int = ++value

    operator fun plus(other: Counter): Counter = Counter().also { it.value = value + other.value }
}

// FILE: discardable_result_marked.kt
@file:MustUseReturnValues

fun checkable(): Int = ++calls
