/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun interface Foo<T> {
    fun bar(x: T): Int
}

fun baz(x: Any): Int = x.hashCode()

// CHECK-LABEL: define void @"kfun:#main(){}"()
// CHECK-NOT: Int-box
// CHECK-NOT: Int-unbox
// CHECK: ret void
fun main() {
    val foo: Foo<Int> = Foo(::baz)
    if( foo.bar(42) == 42 )
        println("passed")
}
