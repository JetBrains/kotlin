package com.h0tk3y.hmpp.klib.demo

actual class LibCommonMainExpect : LibCommonMainIface {
    actual fun libCommonMainExpectFun(): Unit {
        println("actualized in jvmAndJsMain")
        libCommonMainTopLevelFun()
    }

    fun additionalFunInJvmAndJsActual() {
        println("additional fun from jvmAndJsMain")
    }
}