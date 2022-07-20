/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun interface Foo {
    fun bar(x: Int): Any
}

fun baz(x: Any): Int = x.hashCode()

// CHECK-LABEL: define void @"kfun:#main(){}"()
// Boxing needs to be used now due to non-devirtualized call
// CHECK: Int-box

// CHECK-NOT: Int-box
// CHECK-NOT: Int-unbox
// CHECK: ret void

fun main() {
    val foo: Foo = Foo(::baz)
    if( foo.bar(42) == 42 )
        println("passed")
}
