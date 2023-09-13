/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.cinterop.*

// CHECK-LABEL: define {{zeroext i1|i1}} @"kfun:kotlinx.cinterop.CPointer#equals(kotlin.Any?){}kotlin.Boolean"(i8* %0, %struct.ObjHeader* %1)
// CHECK: call i8* @"kfun:kotlinx.cinterop#<CPointer-unbox>(kotlin.Any?){}kotlinx.cinterop.CPointer<-1:0>?"

fun main() = memScoped {
    val var1: IntVar = alloc()
    val var2: IntVar = alloc()
    println(var1.ptr == var2.ptr)
}
