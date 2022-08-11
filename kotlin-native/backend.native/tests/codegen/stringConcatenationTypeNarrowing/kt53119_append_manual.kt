/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.stringConcatenationTypeNarrowing.kt53119_append_manual
import kotlin.test.*

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual#appendMaybeAny(kotlin.Any?)
// CHECK: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append

// CHECK: ret %struct.ObjHeader*

fun appendMaybeAny(maybeAny: Any?): String {
    val sb = kotlin.text.StringBuilder()
    sb.append(maybeAny)
    return sb.toString()
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual#appendAny(kotlin.Any)
// CHECK: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)

// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append
// CHECK: ret %struct.ObjHeader*

fun appendAny(any: Any): String {
    val sb = kotlin.text.StringBuilder()
    sb.append(any)
    return sb.toString()
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual#appendMaybeString(kotlin.String?)
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append

// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String

// CHECK: ret %struct.ObjHeader*

fun appendMaybeString(maybeStr: String?): String {
    val sb = kotlin.text.StringBuilder()
    sb.append(maybeStr)
    return sb.toString()
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual#appendString(kotlin.String)
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append

// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String

// CHECK: ret %struct.ObjHeader*

fun appendString(str: String): String {
    val sb = kotlin.text.StringBuilder()
    sb.append(str)
    return sb.toString()
}

data class Foo(val bar: Int)

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual#appendFoo(codegen.stringConcatenationTypeNarrowing.kt53119_append_manual.Foo)
// CHECK: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)

// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append
// CHECK: ret %struct.ObjHeader*

fun appendFoo(foo: Foo): String {
    val sb = kotlin.text.StringBuilder()
    sb.append(foo)
    return sb.toString()
}

@Test
fun runTest() {
    val foo = Foo(42)
    println(appendMaybeAny(foo))
    println(appendMaybeAny(null))
    println(appendAny(foo))
    println(appendMaybeString("foo"))
    println(appendMaybeString(null))
    println(appendString("foo"))
    println(appendFoo(foo))
}
