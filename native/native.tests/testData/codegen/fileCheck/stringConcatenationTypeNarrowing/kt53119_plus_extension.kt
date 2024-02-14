// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

package codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension
import kotlin.test.*

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension#manualPlusExtensionAny
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK-OPT: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"

// CHECK-OPT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)
// CHECK-OPT-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl

// CHECK: ret %struct.ObjHeader*

fun manualPlusExtensionAny(maybeStr: String?, maybeAny: kotlin.Any?): kotlin.String =
        maybeStr + maybeAny

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension#manualPlusExtensionString
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK-OPT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl

// CHECK-OPT-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun manualPlusExtensionString(maybeStr: String?, str: String): kotlin.String =
        maybeStr + str

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension#generatedPlusExtensionAny
// CHECK-OPT-NOT: kfun:kotlin#plus__at__kotlin.String?(kotlin.Any?)

// CHECK-OPT: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: kfun:kotlin#plus__at__kotlin.String?(kotlin.Any?)
// CHECK-OPT-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"

// CHECK: ret %struct.ObjHeader*

fun generatedPlusExtensionAny(maybeStr: String?, maybeAny: Any?): String {
    return "$maybeStr$maybeAny"
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension#generatedPlusExtensionString
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK-OPT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl

// CHECK-OPT-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun generatedPlusExtensionString(maybeStr: String?, str: String): String {
    return "$maybeStr$str"
}

data class Foo(val bar: Int)

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension#generatedPlusExtensionFoo
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK-OPT: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String

// CHECK-OPT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl

// CHECK-OPT-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun generatedPlusExtensionFoo(maybeStr: String?, foo: Foo): String {
    return "$maybeStr$foo"
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension#generatedPlusExtensionMaybeFoo
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK-OPT: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_extension.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String
// CHECK-OPT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"

// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun generatedPlusExtensionMaybeFoo(maybeStr: String?, foo: Foo?): String {
    return "$maybeStr$foo"
}

fun box(): String {
    val foo = Foo(42)
    val res1 = manualPlusExtensionAny("foo", foo)
    if (res1 != "fooFoo(bar=42)") return "FAIL1: $res1"
    val res2 = manualPlusExtensionAny(null, null)
    if (res2 != "nullnull") return "FAIL2: $res2"
    val res3 = manualPlusExtensionString("foo", "bar")
    if (res3 != "foobar") return "FAIL3: $res3"
    val res4 = manualPlusExtensionString(null, "bar")
    if (res4 != "nullbar") return "FAIL4: $res4"
    val res5 = generatedPlusExtensionAny("foo", foo)
    if (res5 != "fooFoo(bar=42)") return "FAIL5: $res5"
    val res6 = generatedPlusExtensionAny(null, null)
    if (res6 != "nullnull") return "FAIL6: $res6"
    val res7 = generatedPlusExtensionString("foo", "bar")
    if (res7 != "foobar") return "FAIL7: $res7"
    val res8 = generatedPlusExtensionString(null, "bar")
    if (res8 != "nullbar") return "FAIL8: $res8"
    val res9 = generatedPlusExtensionFoo(null, Foo(42))
    if (res9 != "nullFoo(bar=42)") return "FAIL9: $res9"
    val res10 = generatedPlusExtensionMaybeFoo("foo", Foo(42))
    if (res10 != "fooFoo(bar=42)") return "FAIL10: $res10"
    val res11 = generatedPlusExtensionMaybeFoo("foo", null)
    if (res11 != "foonull") return "FAIL11: $res11"
    return "OK"
}
