/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.h0tk3y.hmpp.klib.demo

import kotlinx.cinterop.CArrayPointer

actual class LibCommonMainExpect : LibCommonMainIface {
    actual fun libCommonMainExpectFun(): Unit {
        println("actualized in embeddedMain")
        libCommonMainTopLevelFun()
        println(CArrayPointer::class)
    }

    fun additionalFunInEmbeddedActual() {
        println("additional fun in lib embeddedMain")
    }
}

fun libLinuxMainFun(): LibCommonMainIface = LibCommonMainExpect()