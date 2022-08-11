/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.stringConcatenationTypeNarrowing.kt53119_plus_member
import kotlin.test.*

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member#manualPlusMemberAny
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun manualPlusMemberAny(str: String, maybeAny: kotlin.Any?): kotlin.String =
    str + maybeAny

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member#manualPlusMemberString
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"

// CHECK: ret %struct.ObjHeader*

fun manualPlusMemberString(str1: String, str2: String): kotlin.String =
        str1 + str2

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member#generatedPlusMemberAny
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun generatedPlusMemberAny(str: String, maybeAny: Any?): String {
    return "$str$maybeAny"
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member#generatedPlusMemberString
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl

// CHECK-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun generatedPlusMemberString(str1: String, str2: String): String {
    return "$str1$str2"
}

data class Foo(val bar: Int)

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member#manualPlusMemberFoo
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)
// CHECK call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member.Foo#toString(){}kotlin.String"
// CHECK-NOT Foo#toString(){}kotlin.String

// CHECK: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)
// CHECK-NOT: Foo#toString(){}kotlin.String"

// CHECK: ret %struct.ObjHeader*
fun manualPlusMemberFoo(str1: String, foo: Foo): kotlin.String =
        str1 + foo

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member#manualPlusMemberMaybeFoo
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)
// CHECK call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member.Foo#toString(){}kotlin.String"
// CHECK-NOT Foo#toString(){}kotlin.String

// CHECK: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member.Foo#toString
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)
// CHECK: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: Foo#toString(){}kotlin.String"

// CHECK: ret %struct.ObjHeader*
fun manualPlusMemberMaybeFoo(str1: String, foo: Foo?): kotlin.String =
        str1 + foo

@Test
fun runTest() {
    val foo = Foo(42)
    println(manualPlusMemberAny("foo", foo))
    println(manualPlusMemberAny("foo", null))
    println(manualPlusMemberString("foo", "bar"))
    println(generatedPlusMemberAny("foo", null))
    println(generatedPlusMemberAny("foo", foo))
    println(generatedPlusMemberString("foo", "bar"))
    println(manualPlusMemberFoo("foo", Foo(42)))
    println(manualPlusMemberMaybeFoo("foo", Foo(42)))
    println(manualPlusMemberMaybeFoo("foo", null))
}
