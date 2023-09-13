/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// CHECK-LABEL: define {{zeroext i1|i1}} @"kfun:C#equals(kotlin.Any?){}kotlin.Boolean"(%struct.ObjHeader* %0, %struct.ObjHeader* %1)
// CHECK: call %struct.ObjHeader* @"kfun:#<C-unbox>(kotlin.Any?){}C?"
value class C(val x: Any)
// Note: <C-unbox> is also called from bridges for equals, hashCode and toString.

fun main() {
    println(C(42) == C(13))
}
