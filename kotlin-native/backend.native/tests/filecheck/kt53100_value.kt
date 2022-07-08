/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

value class IntWrapper(val value: Int)
class C {
    fun foo(x: IntWrapper) = x
}

// CHECK: define void @"kfun:#main(){}"()

// TODO Invert next check after "IntWrapper(CONSTANT_PRIMITIVE 42)" will be optimised away
// CHECK: kfun:IntWrapper#<constructor>#static(kotlin.Int){}

// CHECK-NOT: kfun:kotlin#<Int-box>(kotlin.Int){}kotlin.Any
// CHECK-NOT: kfun:kotlin#<Int-unbox>(kotlin.Any){}kotlin.Int
// CHECK: ret void

fun main() {
    val c = C()
    val fooref = c::foo
    if( fooref(IntWrapper(42)).value == 42)
        println("ok")
}