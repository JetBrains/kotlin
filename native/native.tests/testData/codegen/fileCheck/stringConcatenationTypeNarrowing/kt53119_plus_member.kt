// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

package codegen.stringConcatenationTypeNarrowing.kt53119_plus_member
import kotlin.test.*

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member#manualPlusMemberAny
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK-OPT: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun manualPlusMemberAny(str: String, maybeAny: kotlin.Any?): kotlin.String =
    str + maybeAny

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member#manualPlusMemberString
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK-OPT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK-OPT-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"

// CHECK: ret %struct.ObjHeader*

fun manualPlusMemberString(str1: String, str2: String): kotlin.String =
        str1 + str2

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member#generatedPlusMemberAny
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK-OPT: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun generatedPlusMemberAny(str: String, maybeAny: Any?): String {
    return "$str$maybeAny"
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member#generatedPlusMemberString
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK-OPT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: call %struct.ObjHeader* @Kotlin_String_plusImpl

// CHECK-OPT-NOT: call %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String"
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK: ret %struct.ObjHeader*

fun generatedPlusMemberString(str1: String, str2: String): String {
    return "$str1$str2"
}

data class Foo(val bar: Int)

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member#manualPlusMemberFoo
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)
// CHECK-OPT: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT Foo#toString(){}kotlin.String

// CHECK-OPT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"

// CHECK: ret %struct.ObjHeader*
fun manualPlusMemberFoo(str1: String, foo: Foo): kotlin.String =
        str1 + foo

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member#manualPlusMemberMaybeFoo
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)
// CHECK-OPT: call %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_plus_member.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String
// CHECK-OPT-NOT: kfun:kotlin.String#plus(kotlin.Any?)

// CHECK-OPT: call %struct.ObjHeader* @Kotlin_String_plusImpl
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"

// CHECK: ret %struct.ObjHeader*
fun manualPlusMemberMaybeFoo(str1: String, foo: Foo?): kotlin.String =
        str1 + foo

fun box(): String {
    val foo = Foo(42)
    val res1 = manualPlusMemberAny("foo", foo)
    if (res1 != "fooFoo(bar=42)") return "FAIL1: $res1"
    val res2 = manualPlusMemberAny("foo", null)
    if (res2 != "foonull") return "FAIL2: $res2"
    val res3 = manualPlusMemberString("foo", "bar")
    if (res3 != "foobar") return "FAIL3: $res3"
    val res4 = generatedPlusMemberAny("foo", null)
    if (res4 != "foonull") return "FAIL4: $res4"
    val res5 = generatedPlusMemberAny("foo", foo)
    if (res5 != "fooFoo(bar=42)") return "FAIL5: $res5"
    val res6 = generatedPlusMemberString("foo", "bar")
    if (res6 != "foobar") return "FAIL6: $res6"
    val res7 = manualPlusMemberFoo("foo", Foo(42))
    if (res7 != "fooFoo(bar=42)") return "FAIL7: $res7"
    val res8 = manualPlusMemberMaybeFoo("foo", Foo(42))
    if (res8 != "fooFoo(bar=42)") return "FAIL8: $res8"
    val res9 = manualPlusMemberMaybeFoo("foo", null)
    if (res9 != "foonull") return "FAIL9: $res9"
    return "OK"
}
