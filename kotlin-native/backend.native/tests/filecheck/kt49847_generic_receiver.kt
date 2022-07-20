/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// CHECK-LABEL: define internal void @"kfun:$foo$FUNCTION_REFERENCE$0.<init>#internal"
// CHECK-SAME: i32

fun <T> T.foo() { println(this) }

fun main() {
    println(5::foo)
}
