/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

var i = 1
fun main() {
    println(when(i) {
        0 -> 10
        1 -> 11
        2 -> 12
        else -> 13
    })
}
// CHECK-LABEL: define void @"kfun:#main(){}"()
// CHECK: when_case
// CHECK: when_next
// CHECK: when_case1
// CHECK: when_next2
// CHECK: when_case3
// CHECK: when_next4
// CHECK: when_exit
// CHECK: call void @"kfun:kotlin.io#println(kotlin.Any?)
// CHECK: ret void
