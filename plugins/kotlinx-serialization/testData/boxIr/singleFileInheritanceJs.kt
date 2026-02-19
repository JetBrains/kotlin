// ISSUE: KT-77681
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^^^ KT-77681: [IR VALIDATION] IrValidationBeforeLoweringPhase: The following expression references a value that is not available in the current scope.
//               GET_VAR '<this>: <root>.OpenBody declared in <root>.OpenBody' type=<root>.OpenBody origin=IMPLICIT_ARGUMENT

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.assertEquals

@Serializable
open class OpenBody {
    var optional: String = "foo"
    val field2: String = optional
}

@Serializable
class Test1: OpenBody()

fun test1() {
    val json = Json { encodeDefaults = true }
    val string = json.encodeToString(Test1.serializer(), Test1())
    assertEquals("{\"optional\":\"foo\",\"field2\":\"foo\"}", string)
    val reconstructed = json.decodeFromString(Test1.serializer(), string)
    assertEquals("foo", reconstructed.field2)
}

fun box(): String {
    test1()
    return "OK"
}
