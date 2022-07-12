/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun interface Foo {
    fun bar(x: Int): Any
}

fun baz(x: Any): Int = x.hashCode()

// CHECK: define void @"kfun:#main(){}"()
// Boxing/unboxing need to be used now due to non-devirtualized call
// CHECK: Int-box

//  TODO  Remove two next checks, when advanced optimization of Int-unbox(Int-box(x)) would be done for snippet like:
//  TODO  VAR IR_TEMPORARY_VARIABLE name:arg0 type:kotlin.Any [val]
//  TODO    BLOCK type=kotlin.Any origin=null
//  TODO      CALL <Int-box>
//  TODO        GET_VAR 'val arg1: kotlin.Int [val]'
//  TODO  CALL <Int-unbox>
//  TODO    GET_VAR 'val arg0: kotlin.Any [val]'
// CHECK: Int-box
// CHECK: Int-unbox

// CHECK-NOT: Int-box
// CHECK-NOT: Int-unbox
// CHECK: ret void

fun main() {
    val foo: Foo = Foo(::baz)
    if( foo.bar(42) == 42 )
        println("passed")
}
