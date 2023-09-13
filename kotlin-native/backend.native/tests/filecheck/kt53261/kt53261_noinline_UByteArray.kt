/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// CHECK-LABEL: define {{zeroext i1|i1}} @"kfun:kotlin.UByteArray#equals(kotlin.Any?){}kotlin.Boolean"(%struct.ObjHeader* %0, %struct.ObjHeader* %1)
// CHECK: call %struct.ObjHeader* @"kfun:kotlin#<UByteArray-unbox>(kotlin.Any?){}kotlin.UByteArray?"

fun main() {
    val arr1 = UByteArray(10) { it.toUByte() }
    val arr2 = UByteArray(10) { (it / 2).toUByte() }
    println(arr1 == arr2)
}
