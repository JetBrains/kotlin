package com.h0tk3y.hmpp.klib.demo

import kotlinx.cinterop.CArrayPointer

actual class LibCommonMainExpect : LibCommonMainIface {
    actual fun libCommonMainExpectFun(): Unit {
        println("actualized in iosMain")
        libCommonMainTopLevelFun()
        println(CArrayPointer::class)
    }

    fun additionalFunInIosActual() {
        println("additional fun in lib iosMain")
    }
}

fun libIosMainFun(): LibCommonMainIface = LibCommonMainExpect()