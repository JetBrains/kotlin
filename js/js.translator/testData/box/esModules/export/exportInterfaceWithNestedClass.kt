// RUN_PLAIN_BOX_FUNCTION
// ES_MODULES

// MODULE: export_interface_with_nested_class
// FILE: lib.kt

@JsExport
interface I {
    companion object {
        fun foo() = 42
    }

    fun createNested(): Nested = Nested()

    class Nested(val value: String = "OK") {
        class DeepNested(val value: String)

        abstract class AbstractNested {
            abstract fun box(): String
        }

        class ConcreteNested : AbstractNested() {
            override fun box() = "OK"
        }

        fun box() = value

        fun companionValue() = foo()
    }

    class WithSecondaryConstructor(val value: String) {
        @JsName("defaultValue")
        constructor(): this("OK")
    }

    class WithDefaultsAndVarargs(val prefix: String = "", vararg val parts: String) {
        fun box() = prefix + parts[0]
    }

    class GenericNested<T>(val value: T)

    data class DataNested(val value: String)

    class WithCompanion(val value: String) {
        companion object {
            @JsStatic
            fun create(value: String) = WithCompanion(value)
        }
    }
}

@JsExport
fun createNestedWithInterfaceDefault(): String =
    object : I {}.createNested().box()

@JsExport
fun interface FI {
    fun run(value: String): String

    class Nested(val value: String) {
        fun box() = value
    }
}

// FILE: main.mjs
// ENTRY_ES_MODULE

import { createNestedWithInterfaceDefault, FI, I } from "./exportInterfaceWithNestedClass-export_interface_with_nested_class_v5.mjs"

export function box() {
    const nested = new I.Nested()

    if (nested.value !== "OK") return "Fail: nested class constructor default was not exported"
    if (nested.box() !== "OK") return "Fail: nested class function was not exported"
    if (createNestedWithInterfaceDefault() !== "OK") return "Fail: interface default cannot create nested class"

    const deepNested = new I.Nested.DeepNested("deep")
    if (deepNested.value !== "deep") return "Fail: deep nested class was not exported"

    const concrete = new I.Nested.ConcreteNested()
    if (concrete.box() !== "OK") return "Fail: nested abstract class inheritor was not exported"

    if (nested.companionValue() !== 42) return "Fail: nested class cannot access interface companion"

    const secondary = I.WithSecondaryConstructor.defaultValue()
    if (secondary.value !== "OK") return "Fail: nested class secondary constructor was not exported"

    const withDefaultsAndVarargs = new I.WithDefaultsAndVarargs("O", ["K"])
    if (withDefaultsAndVarargs.box() !== "OK") return "Fail: nested class with defaults and varargs was not exported"

    const withDefaultValue = new I.WithDefaultsAndVarargs(undefined, ["OK"])
    if (withDefaultValue.box() !== "OK") return "Fail: nested class constructor default with varargs was not exported"

    const generic = new I.GenericNested("OK")
    if (generic.value !== "OK") return "Fail: nested generic class was not exported"

    const data = new I.DataNested("fail").copy("OK")
    if (data.value !== "OK") return "Fail: nested data class was not exported"

    const withCompanion = I.WithCompanion.create("OK")
    if (withCompanion.value !== "OK") return "Fail: nested class companion static function was not exported"

    const funInterfaceNested = new FI.Nested("OK")
    if (funInterfaceNested.box() !== "OK") return "Fail: nested class in fun interface was not exported"

    return "OK"
}
