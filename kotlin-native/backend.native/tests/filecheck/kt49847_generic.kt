/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class C<T> {
    fun foo(x: T) = x
}

// CHECK: define void @"kfun:#main(){}"()
// CHECK-NOT: kfun:kotlin#<Int-box>(kotlin.Int){}kotlin.Any
// TODO Invert next check after fix of https://youtrack.jetbrains.com/issue/KT-53100/Optimization-needed-T-unboxCONSTANTPRIMITIVEx-T-x
// CHECK: kfun:kotlin#<Int-unbox>(kotlin.Any){}kotlin.Int
// CHECK: ret void
fun main() {
    val c = C<Int>()
    val fooref = c::foo
    if( fooref(42) == 42)
        println("ok")
}
