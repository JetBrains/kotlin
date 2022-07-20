/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class C {
    fun foo(x: Int) = x
}

// CHECK-LABEL: define void @"kfun:#main(){}"()
// CHECK-NOT: Int-box
//   TODO Remove next check after fix of https://youtrack.jetbrains.com/issue/KT-53100/Optimization-needed-T-unboxCONSTANTPRIMITIVEx-T-x
// CHECK: Int-unbox
// CHECK-NOT: Int-unbox
// CHECK: ret void
fun main() {
    val c = C()
    val fooref = c::foo
    if( fooref(42) == 42)
        println("ok")
}
