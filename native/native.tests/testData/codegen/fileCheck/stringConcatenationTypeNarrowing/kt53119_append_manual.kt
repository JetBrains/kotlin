// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

package codegen.stringConcatenationTypeNarrowing.kt53119_append_manual
import kotlin.test.*

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual#appendMaybeAny(kotlin.Any?)
// CHECK-OPT: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append

// CHECK: ret %struct.ObjHeader*

fun appendMaybeAny(maybeAny: Any?): String {
    val sb = kotlin.text.StringBuilder()
    sb.append(maybeAny)
    return sb.toString()
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual#appendAny(kotlin.Any)
// CHECK-OPT: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)

// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append
// CHECK: ret %struct.ObjHeader*

fun appendAny(any: Any): String {
    val sb = kotlin.text.StringBuilder()
    sb.append(any)
    return sb.toString()
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual#appendMaybeString(kotlin.String?)
// CHECK-OPT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append

// CHECK-OPT-NOT: %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String

// CHECK: ret %struct.ObjHeader*

fun appendMaybeString(maybeStr: String?): String {
    val sb = kotlin.text.StringBuilder()
    sb.append(maybeStr)
    return sb.toString()
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual#appendString(kotlin.String)
// CHECK-OPT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)
// CHECK-OPT-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append

// CHECK-OPT-NOT: %struct.ObjHeader* @"kfun:kotlin.String#toString(){}kotlin.String

// CHECK: ret %struct.ObjHeader*

fun appendString(str: String): String {
    val sb = kotlin.text.StringBuilder()
    sb.append(str)
    return sb.toString()
}

data class Foo(val bar: Int)

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual#appendFoo(codegen.stringConcatenationTypeNarrowing.kt53119_append_manual.Foo)
// CHECK-OPT: %struct.ObjHeader* @"kfun:codegen.stringConcatenationTypeNarrowing.kt53119_append_manual.Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append(kotlin.String?)

// CHECK-OPT-NOT: Foo#toString(){}kotlin.String"
// CHECK-OPT-NOT: %struct.ObjHeader* @"kfun:kotlin.text.StringBuilder#append
// CHECK: ret %struct.ObjHeader*

fun appendFoo(foo: Foo): String {
    val sb = kotlin.text.StringBuilder()
    sb.append(foo)
    return sb.toString()
}

fun box(): String {
    val foo = Foo(42)
    val res1 = appendMaybeAny(foo)
    if (res1 != "Foo(bar=42)") return "FAIL1: $res1"
    val res2 = appendMaybeAny(null)
    if (res2 != "null") return "FAIL2: $res2"
    val res3 = appendAny(foo)
    if (res3 != "Foo(bar=42)") return "FAIL3: $res3"
    val res4 = appendMaybeString("foo")
    if (res4 != "foo") return "FAIL4: $res4"
    val res5 = appendMaybeString(null)
    if (res5 != "null") return "FAIL5: $res5"
    val res6 = appendString("foo")
    if (res6 != "foo") return "FAIL6: $res6"
    val res7 = appendFoo(foo)
    if (res7 != "Foo(bar=42)") return "FAIL7: $res7"
    return "OK"
}
