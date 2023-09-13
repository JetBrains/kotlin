/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// CHECK-LABEL: define {{zeroext i1|i1}} @"kfun:kotlin.ULongArray#equals(kotlin.Any?){}kotlin.Boolean"(%struct.ObjHeader* %0, %struct.ObjHeader* %1)
// CHECK: call %struct.ObjHeader* @"kfun:kotlin#<ULongArray-unbox>(kotlin.Any?){}kotlin.ULongArray?"

fun main() {
    val arr1 = ULongArray(10) { it.toULong() }
    val arr2 = ULongArray(10) { (it / 2).toULong() }
    println(arr1 == arr2)
}
