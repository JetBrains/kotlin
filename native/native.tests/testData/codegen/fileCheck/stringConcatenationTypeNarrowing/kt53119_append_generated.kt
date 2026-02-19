// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

package codegen.stringConcatenationTypeNarrowing.kt53119_append_generated
import kotlin.test.*

// CHECK-LABEL: define ptr @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated#maybeAnyMaybeAny
// CHECK-OPT: ptr @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT-NOT: ptr @"kfun:kotlin.text.StringBuilder#append

// CHECK-OPT: ptr @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT-NOT: ptr @"kfun:kotlin.text.StringBuilder#append

// CHECK: ret ptr

fun maybeAnyMaybeAny(maybeAny1: Any?, maybeAny2: Any?): String {
    return "$maybeAny1,$maybeAny2"
}

// CHECK-LABEL: define ptr @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated#maybeAnyMaybeString
// CHECK-OPT: ptr @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT-NOT: ptr @"kfun:kotlin.text.StringBuilder#append

// CHECK-OPT-NOT: ptr @"kfun:kotlin.text.StringBuilder#append
// CHECK: ret ptr

fun maybeAnyMaybeString(maybeAny1: Any?, maybeString2: String?): String {
    return "$maybeAny1,$maybeString2"
}

// CHECK-LABEL: define ptr @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated#maybeAnyString
// CHECK-OPT: ptr @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT-NOT: ptr @"kfun:kotlin.text.StringBuilder#append

// CHECK-OPT-NOT: ptr @"kfun:kotlin.text.StringBuilder#append
// CHECK: ret ptr

fun maybeAnyString(maybeAny1: Any?, string: String): String {
    return "$maybeAny1,$string"
}

data class Foo(val bar: Int)

// CHECK-LABEL: define ptr @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated#maybeAnyFoo
// CHECK-OPT: ptr @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT-NOT: ptr @"kfun:kotlin.text.StringBuilder#append
// CHECK-OPT: ptr @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT-NOT: ptr @"kfun:kotlin.text.StringBuilder#append

// CHECK-OPT-NOT: ptr @"kfun:kotlin.text.StringBuilder#append
// CHECK: ret ptr

fun maybeAnyFoo(maybeAny: Any?, foo: Foo): String {
    return "$maybeAny,$foo"
}
// CHECK-LABEL: define ptr @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated#maybeAnyMaybeFoo
// CHECK-OPT: ptr @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT: ptr @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_generated.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT: ptr @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT-NOT: ptr @"kfun:kotlin.text.StringBuilder#append


// CHECK-OPT-NOT: ptr @"kfun:kotlin.text.StringBuilder#append
// CHECK: ret ptr

fun maybeAnyMaybeFoo(maybeAny: Any?, foo: Foo?): String {
    return "$maybeAny,$foo"
}

fun box(): String {
    val foo = Foo(42)
    val res1 = maybeAnyMaybeAny(foo, foo)
    if (res1 != "Foo(bar=42),Foo(bar=42)") return "FAIL1: $res1"
    val res2 = maybeAnyMaybeAny(null, null)
    if (res2 != "null,null") return "FAIL2: $res2"
    val res3 = maybeAnyMaybeString(foo, "bar")
    if (res3 != "Foo(bar=42),bar") return "FAIL3: $res3"
    val res4 = maybeAnyMaybeString(null, null)
    if (res4 != "null,null") return "FAIL4: $res4"
    val res5 = maybeAnyString(foo, "bar")
    if (res5 != "Foo(bar=42),bar") return "FAIL5: $res5"
    val res6 = maybeAnyString(null, "bar")
    if (res6 != "null,bar") return "FAIL6: $res6"
    val res7 = maybeAnyFoo(foo, foo)
    if (res7 != "Foo(bar=42),Foo(bar=42)") return "FAIL7: $res7"
    val res8 = maybeAnyFoo(null, foo)
    if (res8 != "null,Foo(bar=42)") return "FAIL8: $res8"
    val res9 = maybeAnyMaybeFoo(foo, foo)
    if (res9 != "Foo(bar=42),Foo(bar=42)") return "FAIL9: $res9"
    val res10 = maybeAnyMaybeFoo(foo, null)
    if (res10 != "Foo(bar=42),null") return "FAIL10: $res10"
    return "OK"
}
