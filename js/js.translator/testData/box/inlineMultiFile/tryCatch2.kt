// EXPECTED_REACHABLE_NODES: 517
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/tryCatchFinally/tryCatch2.1.kt
 */

// FILE: a.kt
package foo

fun test1(): Int {
    val res = My(111).performWithFail<My, Int>(
            {
                throw RuntimeExceptionWithValue()
            }, {
                it.value
            })
    return res
}

fun test11(): Int {
    val res = My(111).performWithFail2<My, Int>(
            {
                try {
                    throw RuntimeExceptionWithValue("1")
                } catch (e: RuntimeExceptionWithValue) {
                    throw RuntimeExceptionWithValue("2")
                }
            },
            { ex, thizz ->
                if (ex.value == "2") {
                    thizz.value
                } else {
                    -11111
                }
            })
    return res
}

fun test2(): Int {
    val res = My(111).performWithFail<My, Int>(
            {
                it.value
            },
            {
                it.value + 1
            })
    return res
}

fun test22(): Int {
    val res = My(111).performWithFail2<My, Int>(
            {
                try {
                    throw RuntimeExceptionWithValue("1")
                } catch (e: RuntimeExceptionWithValue) {
                    it.value
                    111
                }
            },
            { ex, thizz ->
                -11111
            })

    return res
}


fun test3(): Int {
    try {
        val res = My(111).performWithFail<My, Int>(
                {
                    throw RuntimeExceptionWithValue("-1")
                }, {
                    throw RuntimeExceptionWithValue("-2")
                })
        return res
    } catch (e: RuntimeExceptionWithValue) {
        return e.value.toInt2()!!
    }
}

fun test33(): Int {
    try {
        val res = My(111).performWithFail2<My, Int>(
                {
                    try {
                        throw RuntimeExceptionWithValue("-1")
                    } catch (e: RuntimeExceptionWithValue) {
                        throw RuntimeExceptionWithValue("-2")
                    }
                },
                { ex, thizz ->
                    if (ex.value == "-2") {
                        throw RuntimeExceptionWithValue("-3")
                    } else {
                        -11111
                    }
                })
        return res
    } catch (e: RuntimeExceptionWithValue) {
        return e.value.toInt2()!!
    }
}

fun box(): String {
    if (test1() != 111) return "test1: ${test1()}"
    if (test11() != 111) return "test11: ${test11()}"

    if (test2() != 111) return "test2: ${test2()}"
    if (test22() != 111) return "test22: ${test22()}"

    if (test3() != -2) return "test3: ${test3()}"
    if (test33() != -3) return "test33: ${test33()}"

    return "OK"
}


// FILE: b.kt
package foo

class My(val value: Int)

inline fun <T, R> T.performWithFail(job: (T)-> R, failJob: (T) -> R): R {
    try {
        return job(this)
    } catch (e: RuntimeExceptionWithValue) {
        return failJob(this)
    }
}

inline fun <T, R> T.performWithFail2(job: (T)-> R, failJob: (e: RuntimeExceptionWithValue, T) -> R): R {
    try {
        return job(this)
    } catch (e: RuntimeExceptionWithValue) {
        return failJob(e, this)
    }
}

external object Number {
    fun parseInt(str: String): Int = definedExternally
}

inline fun String.toInt2(): Int = this.toInt()

class RuntimeExceptionWithValue(val value: String = "") : RuntimeException()