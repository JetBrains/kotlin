/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension
import kotlin.test.*

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension#manualPlusExtensionAny
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"

// CHECK: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)
// CHECK-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl

// CHECK: ret %struct.ObjHeader*

fun manualPlusExtensionAny(maybeStr: String?, maybeAny: kotlin.Any?): kotlin.String =
        maybeStr + maybeAny

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension#manualPlusExtensionString
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl

// CHECK-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun manualPlusExtensionString(maybeStr: String?, str: String): kotlin.String =
        maybeStr + str

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension#generatedPlusExtensionAny
// CHECK-NOT: kfun:kotlin#plus__at__kotlin.String?(kotlin.Any?)

// CHECK: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String"
// CHECK: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: kfun:kotlin#plus__at__kotlin.String?(kotlin.Any?)
// CHECK-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"

// CHECK: ret %struct.ObjHeader*

fun generatedPlusExtensionAny(maybeStr: String?, maybeAny: Any?): String {
    return "$maybeStr$maybeAny"
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension#generatedPlusExtensionString
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl

// CHECK-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun generatedPlusExtensionString(maybeStr: String?, str: String): String {
    return "$maybeStr$str"
}

data class Foo(val bar: Int)

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension#generatedPlusExtensionFoo
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String

// CHECK: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl

// CHECK-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun generatedPlusExtensionFoo(maybeStr: String?, foo: Foo): String {
    return "$maybeStr$foo"
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension#generatedPlusExtensionMaybeFoo
// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension.Foo#toString(){}kotlin.String"
// CHECK-NOT: Foo#toString(){}kotlin.String
// CHECK: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"

// CHECK-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun generatedPlusExtensionMaybeFoo(maybeStr: String?, foo: Foo?): String {
    return "$maybeStr$foo"
}

@Test
fun runTest() {
    val foo = Foo(42)
    println(manualPlusExtensionAny("foo", foo))
    println(manualPlusExtensionAny(null, null))
    println(manualPlusExtensionString("foo", "bar"))
    println(manualPlusExtensionString(null, "bar"))
    println(generatedPlusExtensionAny("foo", foo))
    println(generatedPlusExtensionAny(null, null))
    println(generatedPlusExtensionString("foo", "bar"))
    println(generatedPlusExtensionString(null, "bar"))
    println(generatedPlusExtensionFoo(null, Foo(42)))
    println(generatedPlusExtensionMaybeFoo("foo", Foo(42)))
    println(generatedPlusExtensionMaybeFoo("foo", null))
}
