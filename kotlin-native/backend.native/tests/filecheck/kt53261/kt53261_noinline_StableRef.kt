/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.cinterop.*

// CHECK-LABEL: define {{zeroext i1|i1}} @"kfun:kotlinx.cinterop.StableRef#equals(kotlin.Any?){}kotlin.Boolean"(i8* %0, %struct.ObjHeader* %1)
// CHECK: call i8* @"kfun:kotlinx.cinterop#<StableRef-unbox>(kotlin.Any?){}kotlinx.cinterop.StableRef<-1:0>?"

fun main() {
    val ref1 = StableRef.create(Any())
    val ref2 = StableRef.create(Any())
    println(ref1 == ref2)
    ref2.dispose()
    ref1.dispose()
}
