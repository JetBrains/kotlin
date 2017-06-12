// EXPECTED_REACHABLE_NODES: 513
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/anonymousObject/anonymousObjectOnDeclarationSiteSuperParams.1.kt
 */

// FILE: foo.kt
package foo

import test.*

fun test1(): String {
    val o = "O"

    val result = doWork ({o}, {"K"}, "11")

    return result.getO() + result.getK() + result.param
}

fun test2() : String {
    //same names as in object
    val o1 = "O"
    val k1 = "K"
    val param = "11"
    val result = doWorkInConstructor ({o1}, {k1}, {param})

    return result.getO() + result.getK() + result.param
}

fun box() : String {
    val result1 = test1();
    if (result1 != "OK11") return "fail1 $result1"

    val result2 = test2();
    if (result2 != "OK11") return "fail2 $result2"

    return "OK"
}


// FILE: test.kt
package test


abstract class A<R>(val param : R) {
    abstract fun getO() : R

    abstract fun getK() : R
}

inline fun <R> doWork(crossinline jobO: ()-> R, crossinline jobK: ()-> R, param: R) : A<R> {
    val s = object : A<R>(param) {

        override fun getO(): R {
            return jobO()
        }
        override fun getK(): R {
            return  jobK()
        }
    }
    return s;
}

inline fun <R> doWorkInConstructor(crossinline jobO: ()-> R, crossinline jobK: ()-> R, crossinline param: () -> R) : A<R> {
    val s = object : A<R>(param()) {
        val o1 = jobO()

        val k1 = jobK()

        override fun getO(): R {
            return o1
        }
        override fun getK(): R {
            return k1
        }
    }
    return s;
}