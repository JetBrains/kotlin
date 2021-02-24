package com.h0tk3y.hmpp.klib.demo

actual class LibCommonMainExpect : LibCommonMainIface {
    actual fun libCommonMainExpectFun(): Unit {
        println("actualized in jvmAndJsMain")
        libCommonMainTopLevelFun()
        libCommonMainInternalFun()

        /** Test KT-37832 */
        throw MyCustomException()
    }

    fun additionalFunInJvmAndJsActual() {
        println("additional fun from jvmAndJsMain")
    }
}