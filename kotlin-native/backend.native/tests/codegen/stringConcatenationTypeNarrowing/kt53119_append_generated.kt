/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.stringConcatenationTypeNarrowing.kt53119_append_generated
import kotlin.test.*

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated#maybeAnyMaybeAny
// CHECK: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append

// CHECK: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append

// CHECK: ret %struct.ObjHeader*

fun maybeAnyMaybeAny(maybeAny1: Any?, maybeAny2: Any?): String {
    return "$maybeAny1,$maybeAny2"
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated#maybeAnyMaybeString
// CHECK: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append

// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append
// CHECK: ret %struct.ObjHeader*

fun maybeAnyMaybeString(maybeAny1: Any?, maybeString2: String?): String {
    return "$maybeAny1,$maybeString2"
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated#maybeAnyString
// CHECK: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append

// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append
// CHECK: ret %struct.ObjHeader*

fun maybeAnyString(maybeAny1: Any?, string: String): String {
    return "$maybeAny1,$string"
}

data class Foo(val bar: Int)

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated#maybeAnyFoo
// CHECK: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append
// CHECK: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append

// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append
// CHECK: ret %struct.ObjHeader*

fun maybeAnyFoo(maybeAny: Any?, foo: Foo): String {
    return "$maybeAny,$foo"
}
// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated#maybeAnyMaybeFoo
// CHECK: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append


// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append
// CHECK: ret %struct.ObjHeader*

fun maybeAnyMaybeFoo(maybeAny: Any?, foo: Foo?): String {
    return "$maybeAny,$foo"
}
@Test
fun runTest() {
    val foo = Foo(42)
    println(maybeAnyMaybeAny(foo, foo))
    println(maybeAnyMaybeAny(null, null))
    println(maybeAnyMaybeString(foo, "bar"))
    println(maybeAnyMaybeString(null, null))
    println(maybeAnyString(foo, "bar"))
    println(maybeAnyString(null, "bar"))
    println(maybeAnyFoo(foo, foo))
    println(maybeAnyFoo(null, foo))
    println(maybeAnyMaybeFoo(foo, foo))
    println(maybeAnyMaybeFoo(foo, null))
}
