// EXPECTED_REACHABLE_NODES: 500
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/anonymousObject/anonymousObjectOnCallSite.1.kt
 */

// FILE: foo.kt
package foo

import test.*

fun box() : String {
    val o = "O"
    val p = "GOOD"
    val result = doWork {
        val k = "K"
        val s = object : A<String>() {

            val param = p;

            override fun getO(): String {
                return o;
            }

            override fun getK(): String {
                return k;
            }
        }

        s.getO() + s.getK() + s.param
    }

    if (result != "OKGOOD") return "fail $result"

    return "OK"
}


// FILE: test.kt
package test

abstract class A<R> {
    abstract fun getO() : R

    abstract fun getK() : R
}


inline fun <R> doWork(job: ()-> R) : R {
    return job()
}

