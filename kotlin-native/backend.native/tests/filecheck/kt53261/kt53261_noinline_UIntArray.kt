/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// CHECK-LABEL: define {{zeroext i1|i1}} @"kfun:kotlin.UIntArray#equals(kotlin.Any?){}kotlin.Boolean"(%struct.ObjHeader* %0, %struct.ObjHeader* %1)
// CHECK: call %struct.ObjHeader* @"kfun:kotlin#<UIntArray-unbox>(kotlin.Any?){}kotlin.UIntArray?"

fun main() {
    val arr1 = UIntArray(10) { it.toUInt() }
    val arr2 = UIntArray(10) { (it / 2).toUInt() }
    println(arr1 == arr2)
}
