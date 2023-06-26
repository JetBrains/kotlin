/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import direct.*
import kotlinx.cinterop.*

//CHECK-LABEL: kfun:#callDirect(){}kotlin.ULong
fun callDirect(): ULong {
    val cc = CallingConventions()
    //CHECK: invoke i64 @_{{[a-zA-Z0-9]+}}_knbridge{{[0-9]+}}(i8* %{{[0-9]+}}, i64 42)
    return cc.direct(42uL)
}

//CHECK-LABEL: kfun:#callRegular(){}kotlin.ULong
fun callRegular(): ULong {
    val cc = CallingConventions()
    //CHECK: invoke i64 @_{{[a-zA-Z0-9]+}}_knbridge{{[0-9]+}}(i8* %{{[0-9]+}}, i8* %{{[0-9]+}}, i64 42)
    return cc.regular(42uL)
}

fun main() {
    callDirect()
    callRegular()
}