/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// CHECK-LABEL: define i64 @"kfun:#foo(){}kotlin.Long"()
fun foo(): Long {
    // CHECK-NOT: @LONG_CACHE
    val data: Map<String, Any> = mapOf()
    return data.getOrElse("id") { 0L } as Long
}
// CHECK: ret i64

inline fun <T> bar(x: T?, f: Boolean): Any {
    when {
        x != null -> return@bar x
        f -> return@bar 0L
        else -> return@bar 1UL
    }
}

// CHECK-LABEL: define i64 @"kfun:#callBar(kotlin.Boolean){}kotlin.ULong
fun callBar(f: Boolean): ULong {
    // CHECK: @LONG_CACHE
    val data: Map<String, Any> = mapOf()
    val x = data["id"]
    return bar(x, f) as ULong
}
// CHECK: ret i64

fun main() {
    println(foo())
    println(callBar(true))
}
