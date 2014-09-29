/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/anonymousObject/anonymousObjectOnCallSiteSuperParams.2.kt
 */

package test

abstract class A<R>(val param: R) {
    abstract fun getO() : R

    abstract fun getK() : R
}


inline fun <R> doWork(job: ()-> R) : R {
    return job()
}