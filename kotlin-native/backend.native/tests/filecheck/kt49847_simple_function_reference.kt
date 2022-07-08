/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun plus1(x: Int) = x + 1

// CHECK: define void @"kfun:#main(){}"()
// CHECK-NOT: kfun:kotlin#<Int-box>(kotlin.Int){}kotlin.Any
// CHECK-NOT: kfun:kotlin#<Int-unbox>(kotlin.Any){}kotlin.Int
// CHECK: ret void
fun main() {
    val ref = ::plus1
    var y = 0
    repeat(100000) {
        y += ref(it)  // Should be devirtualized and invoked without boxing/unboxing (`Int-box`/`Int-unbox`)
    }
    if (y > 999999)
        println("y > 999999")
}
