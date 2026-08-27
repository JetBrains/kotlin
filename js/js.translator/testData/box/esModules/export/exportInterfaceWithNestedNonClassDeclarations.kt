// RUN_PLAIN_BOX_FUNCTION
// ES_MODULES

// MODULE: lib
// FILE: lib.kt

@JsExport
interface I {
    interface Nested {
        val value: String
    }

    object NestedObject {
        val value = "NestedObject"
    }

    enum class NestedEnum(val value: String) {
        A("NestedEnum.A")
    }

    annotation class NestedAnnotation(val value: String)
}

@JsExport
class NestedImplementor(override val value: String) : I.Nested

@JsExport
fun consumeNested(value: I.Nested): String = value.value

// FILE: main.mjs
// ENTRY_ES_MODULE

import { consumeNested, I, NestedImplementor } from "./exportInterfaceWithNestedNonClassDeclarations-lib_v5.mjs"

export function box() {
    if (consumeNested(new NestedImplementor("NestedInterface")) !== "NestedInterface") return "Fail: nested interface type is not usable"
    if (I.NestedObject.value !== "NestedObject") return "Fail: nested object is not exported through the interface namespace"
    if (I.NestedEnum.A.value !== "NestedEnum.A") return "Fail: nested enum is not exported through the interface namespace"
    if (new I.NestedAnnotation("NestedAnnotation").value !== "NestedAnnotation") return "Fail: nested annotation is not exported through the interface namespace"
    return "OK"
}
