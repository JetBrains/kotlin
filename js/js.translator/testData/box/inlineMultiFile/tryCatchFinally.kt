// EXPECTED_REACHABLE_NODES: 511
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/tryCatchFinally/tryCatchFinally.1.kt
 */

// FILE: a.kt
package foo

fun test1(): Int {

    var res = My(111).performWithFinally<My, Int>(
            {
                1
            }, {
                 it.value
            })
    return res
}

fun test11(): Int {
    var result = -1;
    val res = My(111).performWithFinally<My, Int>(
            {
                try {
                    result = it.value
                    throw RuntimeException("1")
                } catch (e: RuntimeException) {
                    ++result
                    throw RuntimeException("2")
                }
            },
            {
                ++result
            })
    return res
}

fun test2(): Int {
    var res = My(111).performWithFinally<My, Int>(
        {
            throw RuntimeException("1")
        },
        {
            it.value
        })


    return res
}

fun test3(): Int {
    try {
        var result = -1;
        val res = My(111).performWithFailFinally<My, Int>(
                {
                    result = it.value;
                    throw RuntimeException("-1")
                },
                { e, z ->
                    ++result
                    throw RuntimeException("-2")
                },
                {
                    ++result
                })
        return res
    } catch (e: RuntimeException) {
        return e.message?.toInt2()!!
    }
}

fun box(): String {
    if (test1() != 111) return "test1: ${test1()}"
    if (test11() != 113) return "test11: ${test11()}"

    if (test2() != 111) return "test2: ${test2()}"

    if (test3() != 113) return "test3: ${test3()}"

    return "OK"
}


// FILE: b.kt
package foo

class My(val value: Int)

inline fun <T, R> T.performWithFinally(job: (T)-> R, finallyFun: (T) -> R) : R {
    try {
        job(this)
    } finally {
        return finallyFun(this)
    }
}

inline fun <T, R> T.performWithFailFinally(job: (T)-> R, failJob : (e: RuntimeException, T) -> R, finallyFun: (T) -> R) : R {
    try {
        job(this)
    } catch (e: RuntimeException) {
        failJob(e, this)
    } finally {
        return finallyFun(this)
    }
}

inline fun String.toInt2() : Int = this.toInt()