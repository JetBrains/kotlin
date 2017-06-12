// EXPECTED_REACHABLE_NODES: 515
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/anonymousObject/anonymousObjectOnDeclarationSite.1.kt
 */

// FILE: foo.kt
package foo

import test.*

fun test1(): String {
    val o = "O"

    val result = doWork ({o}, {"K"}, "GOOD")

    return result.getO() + result.getK() + result.getParam()
}

fun test2() : String {
    //same names as in object
    val o1 = "O"
    val k1 = "K"

    val result = doWorkInConstructor ({o1}, {k1}, "GOOD")

    return result.getO() + result.getK() + result.getParam()
}

fun box() : String {
    val result1 = test1();
    if (result1 != "OKGOOD") return "fail1 $result1"

    val result2 = test2();
    if (result2 != "OKGOOD") return "fail2 $result2"

    return "OK"
}


// FILE: test.kt
package test


abstract class A<R> {
    abstract fun getO() : R

    abstract fun getK() : R

    abstract fun getParam() : R
}

inline fun <R> doWork(crossinline jobO: ()-> R, crossinline jobK: ()-> R, param: R) : A<R> {
    val s = object : A<R>() {

        override fun getO(): R {
            return jobO()
        }
        override fun getK(): R {
            return  jobK()
        }

        override fun getParam(): R {
            return param
        }
    }
    return s;
}

inline fun <R> doWorkInConstructor(crossinline jobO: ()-> R, crossinline jobK: ()-> R, param: R) : A<R> {
    val s = object : A<R>() {

        val p = param;

        val o1 = jobO()

        val k1 = jobK()

        override fun getO(): R {
            return o1
        }
        override fun getK(): R {
            return k1
        }

        override fun getParam(): R {
            return p
        }
    }
    return s;
}