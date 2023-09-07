/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.cinterop.*

// CHECK-LABEL: define i8* @"kfun:kotlinx.cinterop#interpretOpaquePointed(kotlin.native.internal.NativePtr){}kotlinx.cinterop.NativePointed"(i8* %0)
// CHECK: call i8* @"kfun:kotlinx.cinterop#<NativePointed-unbox>(kotlin.Any?){}kotlinx.cinterop.NativePointed?"

fun main() = memScoped {
    val var1: NativePointed = alloc(4, 4)
    println(interpretOpaquePointed(var1.rawPtr))
}
