// IGNORE_NATIVE: targetFamily=IOS
// IGNORE_NATIVE: targetFamily=TVOS
// IGNORE_NATIVE: targetFamily=WATCHOS
// IGNORE_NATIVE: target=macos_x64
// KIND: STANDALONE
// MODULE: Main
// FILE: closures.kt

import kotlinx.coroutines.*

object Foo

// ========== Basic type tests ==========

fun simple(arg: Int, block: suspend (Int) -> Int): Int {
    return runBlocking {
        block(arg)
    }
}

suspend fun simpleSuspend(arg: Int, block: suspend (Int) -> Int): Int {
    return block(arg)
}

fun withLong(arg: Long, block: suspend (Long) -> Long): Long {
    return runBlocking {
        block(arg)
    }
}

fun withDouble(arg: Double, block: suspend (Double) -> Double): Double {
    return runBlocking {
        block(arg)
    }
}

fun withBoolean(arg: Boolean, block: suspend (Boolean) -> Boolean): Boolean {
    return runBlocking {
        block(arg)
    }
}

fun withString(arg: String, block: suspend (String) -> String): String {
    return runBlocking {
        block(arg)
    }
}

fun withAny(arg: Any, block: suspend (Any) -> Any): Any {
    return runBlocking {
        block(arg)
    }
}

fun withObject(arg: Foo, block: suspend (Foo) -> Foo): Foo {
    return runBlocking {
        block(arg)
    }
}

// ========== Optional/nullable type tests ==========

fun withOptionalInt(arg: Int?, block: suspend (Int?) -> Int?): Int? {
    return runBlocking {
        block(arg)
    }
}

fun withOptionalString(arg: String?, block: suspend (String?) -> String?): String? {
    return runBlocking {
        block(arg)
    }
}

fun withOptionalObject(arg: Foo?, block: suspend (Foo?) -> Foo?): Foo? {
    return runBlocking {
        block(arg)
    }
}

// ========== Unit return type tests ==========

fun withUnitReturn(arg: Int, block: suspend (Int) -> Unit): String {
    return runBlocking {
        block(arg)
        "completed"
    }
}

// ========== Multiple parameters tests ==========

fun withTwoParams(a: Int, b: String, block: suspend (Int, String) -> String): String {
    return runBlocking {
        block(a, b)
    }
}

fun withThreeParams(a: Int, b: String, c: Boolean, block: suspend (Int, String, Boolean) -> String): String {
    return runBlocking {
        block(a, b, c)
    }
}

fun withMixedParams(a: Int?, b: String, c: Foo?, block: suspend (Int?, String, Foo?) -> String): String {
    return runBlocking {
        block(a, b, c)
    }
}

// ========== No parameters tests ==========

fun withNoParams(block: suspend () -> Int): Int {
    return runBlocking {
        block()
    }
}

fun withNoParamsUnit(block: suspend () -> Unit): String {
    return runBlocking {
        block()
        "completed"
    }
}

// ========== Multiple closures tests ==========

fun withTwoClosures(a: Int, first: suspend (Int) -> Int, second: suspend (Int) -> Int): Int {
    return runBlocking {
        val r1 = first(a)
        second(r1)
    }
}

fun withClosureChain(initial: Int, closures: List<suspend (Int) -> Int>): Int {
    return runBlocking {
        var result = initial
        for (closure in closures) {
            result = closure(result)
        }
        result
    }
}



fun produceClosureList(): List<suspend (Int) -> Int> {
    return listOf(
        { it + 1 },
        { it * 2 },
        { it + 10 },
    )
}

fun produceNullableClosureList(): List<(suspend (Int) -> Int)?> {
    return listOf(
        null,
        { it }
    )
}


// ========== Nested calls tests ==========

fun nestedCallHelper(depth: Int, block: suspend (Int) -> Int): Int {
    return runBlocking {
        if (depth <= 0) {
            block(depth)
        } else {
            block(depth) + nestedCallHelper(depth - 1, block)
        }
    }
}

fun nestedCall(depth: Int, block: suspend (Int) -> Int): Int {
    return nestedCallHelper(depth, block)
}

// ========== Exception handling tests ==========

fun catchingClosure(block: suspend () -> Int): String {
    return runBlocking {
        try {
            block().toString()
        } catch (e: Exception) {
            "caught: ${e.message}"
        } catch (e: Throwable) {
            "caught throwable: ${e.message}"
        }
    }
}

fun catchingClosureWithArg(arg: Int, block: suspend (Int) -> Int): String {
    return runBlocking {
        try {
            block(arg).toString()
        } catch (e: Exception) {
            "caught: ${e.message}"
        } catch (e: Throwable) {
            "caught throwable: ${e.message}"
        }
    }
}

// ========== Cancellation tests ==========

suspend fun callClosureWithDelay(delayMs: Long, block: suspend () -> Int): Int {
    delay(delayMs)
    return block()
}

suspend fun callClosureCheckingCancellation(block: suspend () -> Int): Int {
    yield()
    return block()
}

fun callClosureInNewScope(block: suspend () -> Int): Int {
    return runBlocking {
        block()
    }
}

// ========== Kotlin-initiated cancellation tests ==========

fun cancelClosureFromKotlin(block: suspend () -> Int): String {
    return runBlocking {
        val result = withTimeoutOrNull(100) {
            block()
        }
        if (result == null) "timed_out" else "completed: $result"
    }
}

fun cancelClosureWithArgFromKotlin(delayMs: Long, block: suspend (Int) -> Int): String {
    return runBlocking {
        val result = withTimeoutOrNull(100) {
            delay(delayMs)
            block(42)
        }
        if (result == null) "timed_out" else "completed: $result"
    }
}

// ========== Closure that calls back into Kotlin ==========

fun closureCallingKotlin(block: suspend (suspend () -> Int) -> Int): Int {
    return runBlocking {
        block {
            delay(10)
            42
        }
    }
}

// ========== Edge cases ==========

fun closureCalledMultipleTimes(times: Int, block: suspend (Int) -> Int): Int {
    return runBlocking {
        var sum = 0
        repeat(times) { i ->
            sum += block(i)
        }
        sum
    }
}

fun closureNeverCalled(shouldCall: Boolean, block: suspend () -> Int): Int {
    return runBlocking {
        if (shouldCall) {
            block()
        } else {
            -1
        }
    }
}

// ========== Collection types ==========

fun withListParam(list: List<Int>, block: suspend (List<Int>) -> List<Int>): List<Int> {
    return runBlocking {
        block(list)
    }
}

fun withMapParam(map: Map<String, Int>, block: suspend (Map<String, Int>) -> Map<String, Int>): Map<String, Int> {
    return runBlocking {
        block(map)
    }
}

