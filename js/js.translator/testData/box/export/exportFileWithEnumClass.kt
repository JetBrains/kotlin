// !LANGUAGE: +EnumEntries
// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: export_enum_class
// FILE: lib.kt
@file:JsExport

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

class OuterClass {
    enum class NestedEnum {
        A,
        B;
    }
}

// FILE: test.js
function box() {
    if (this["export_enum_class"].Foo.A !== this["export_enum_class"].Foo.A) return "fail1"
    if (this["export_enum_class"].Foo.B !== this["export_enum_class"].Foo.B) return "fail2"

    if (this["export_enum_class"].Foo.Companion.baz !== "baz") return "fail3"

    if (this["export_enum_class"].Foo.A.foo !== 0) return "fail4"
    if (this["export_enum_class"].Foo.B.foo !== 1) return "fail5"

    if (this["export_enum_class"].Foo.A.bar("A") !== "A") return "fail6"
    if (this["export_enum_class"].Foo.B.bar("B") !== "B") return "fail7"

    if (this["export_enum_class"].Foo.A.bay() !== "A") return "fail8"
    if (this["export_enum_class"].Foo.B.bay() !== "B") return "fail9"

    if (this["export_enum_class"].Foo.A.constructorParameter !== "aConstructorParameter") return "fail10"
    if (this["export_enum_class"].Foo.B.constructorParameter !== "bConstructorParameter") return "fail11"

    if (this["export_enum_class"].Bar.A.foo !== 0) return "fail12"
    if (this["export_enum_class"].Bar.B.foo !== 1) return "fail13"

    if (this["export_enum_class"].Bar.A.bar("A") !== "A") return "fail14"
    if (this["export_enum_class"].Bar.B.bar("B") !== "B") return "fail15"

    if (this["export_enum_class"].Bar.A.bay() !== "A") return "fail15"
    if (this["export_enum_class"].Bar.B.bay() !== "B") return "fail16"

    if (this["export_enum_class"].Bar.B.constructor.prototype.hasOwnProperty('d')) return "fail17"
    if (this["export_enum_class"].Bar.B.constructor.prototype.hasOwnProperty('huh')) return "fail18"

    if (this["export_enum_class"].Foo.valueOf("A") !== this["export_enum_class"].Foo.A) return "fail19"
    if (this["export_enum_class"].Foo.valueOf("B") !== this["export_enum_class"].Foo.B) return "fail20"

    if (this["export_enum_class"].Foo.values().indexOf(this["export_enum_class"].Foo.A) === -1) return "fail21"
    if (this["export_enum_class"].Foo.values().indexOf(this["export_enum_class"].Foo.B) === -1) return "fail22"

    if (this["export_enum_class"].Foo.A.name !== "A") return "fail23"
    if (this["export_enum_class"].Foo.B.name !== "B") return "fail24"

    if (this["export_enum_class"].Foo.A.ordinal !== 0) return "fail25"
    if (this["export_enum_class"].Foo.B.ordinal !== 1) return "fail26"

    if (this["export_enum_class"].OuterClass.NestedEnum.A.name !== "A") return "fail27"
    if (this["export_enum_class"].OuterClass.NestedEnum.B.name !== "B") return "fail28"

    if (this["export_enum_class"].OuterClass.NestedEnum.A.ordinal !== 0) return "fail29"
    if (this["export_enum_class"].OuterClass.NestedEnum.B.ordinal !== 1) return "fail30"

    if (this["export_enum_class"].Foo.entries !== undefined) return "fail31"

    return "OK"
}