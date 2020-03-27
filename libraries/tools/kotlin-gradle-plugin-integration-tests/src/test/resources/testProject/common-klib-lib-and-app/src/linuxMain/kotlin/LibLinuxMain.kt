/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.h0tk3y.hmpp.klib.demo

import kotlinx.cinterop.CArrayPointer

actual class LibCommonMainExpect : LibCommonMainIface {
    actual fun libCommonMainExpectFun(): Unit {
        println("actualized in linuxMain")
        libCommonMainTopLevelFun()
        println(CArrayPointer::class)
        libCommonMainInternalFun()

        /** Test KT-37832 */
        throw MyCustomException()
    }

    fun additionalFunInLinuxActual() {
        println("additional fun in lib iosMain")
    }
}

fun libLinuxMainFun(): LibCommonMainIface = LibCommonMainExpect()