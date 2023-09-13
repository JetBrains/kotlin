/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// CHECK-LABEL: define {{zeroext i1|i1}} @"kfun:kotlin.UShortArray#equals(kotlin.Any?){}kotlin.Boolean"(%struct.ObjHeader* %0, %struct.ObjHeader* %1)
// CHECK: call %struct.ObjHeader* @"kfun:kotlin#<UShortArray-unbox>(kotlin.Any?){}kotlin.UShortArray?"

fun main() {
    val arr1 = UShortArray(10) { it.toUShort() }
    val arr2 = UShortArray(10) { (it / 2).toUShort() }
    println(arr1 == arr2)
}
