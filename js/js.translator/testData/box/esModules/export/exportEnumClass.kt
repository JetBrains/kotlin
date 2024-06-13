// LANGUAGE: +EnumEntries
// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES

// MODULE: export_enum_class
// FILE: lib.kt

@JsExport
enum class Uninhabited

@JsExport
enum class Foo(val constructorParameter: String) {
    A("aConstructorParameter"),
    B("bConstructorParameter");

    val foo = ordinal

    fun bar(value: String) = value

    fun bay() = name

    companion object {
        val baz = "baz"
    }
}

@JsExport
enum class Bar {
    A,
    B {
        var d = "d"
        init {
            d = "d2"
        }
        fun huh() = "huh"
    };

    val foo = ordinal

    fun bar(value: String) = value

    fun bay() = name
}

@JsExport
class OuterClass {
    enum class NestedEnum {
        A,
        B;
    }
}


// FILE: main.mjs
// ENTRY_ES_MODULE
import { Uninhabited, Foo, Bar, OuterClass } from "./exportEnumClass-export_enum_class_v5.mjs"

export function box() {
    if (Foo.A !== Foo.A) return "fail1"
    if (Foo.B !== Foo.B) return "fail2"

    if (Foo.Companion.baz !== "baz") return "fail3"

    if (Foo.A.foo !== 0) return "fail4"
    if (Foo.B.foo !== 1) return "fail5"

    if (Foo.A.bar("A") !== "A") return "fail6"
    if (Foo.B.bar("B") !== "B") return "fail7"

    if (Foo.A.bay() !== "A") return "fail8"
    if (Foo.B.bay() !== "B") return "fail9"

    if (Foo.A.constructorParameter !== "aConstructorParameter") return "fail10"
    if (Foo.B.constructorParameter !== "bConstructorParameter") return "fail11"

    if (Bar.A.foo !== 0) return "fail12"
    if (Bar.B.foo !== 1) return "fail13"

    if (Bar.A.bar("A") !== "A") return "fail14"
    if (Bar.B.bar("B") !== "B") return "fail15"

    if (Bar.A.bay() !== "A") return "fail15"
    if (Bar.B.bay() !== "B") return "fail16"

    if (Bar.B.constructor.prototype.hasOwnProperty('d')) return "fail17"
    if (Bar.B.constructor.prototype.hasOwnProperty('huh')) return "fail18"

    if (Foo.valueOf("A") !== Foo.A) return "fail19"
    if (Foo.valueOf("B") !== Foo.B) return "fail20"

    if (Foo.values().indexOf(Foo.A) === -1) return "fail21"
    if (Foo.values().indexOf(Foo.B) === -1) return "fail22"

    if (Foo.A.name !== "A") return "fail23"
    if (Foo.B.name !== "B") return "fail24"

    if (Foo.A.ordinal !== 0) return "fail25"
    if (Foo.B.ordinal !== 1) return "fail26"

    if (OuterClass.NestedEnum.A.name !== "A") return "fail27"
    if (OuterClass.NestedEnum.B.name !== "B") return "fail28"

    if (OuterClass.NestedEnum.A.ordinal !== 0) return "fail29"
    if (OuterClass.NestedEnum.B.ordinal !== 1) return "fail30"

    if (Foo.entries !== undefined) return "fail31"

    if (Uninhabited.values().length !== 0) return "Uninhabited.values"

    return "OK"
}