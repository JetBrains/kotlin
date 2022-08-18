/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun main(arr: Array<String>) {
    println(arr[0].toInt() + 1)
}
// CHECK: {{call|invoke}} %struct.ObjHeader* @"kfun:kotlin#<UShortArray-unbox>
