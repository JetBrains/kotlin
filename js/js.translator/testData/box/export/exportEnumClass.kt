// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: export-enum-class
// FILE: lib.kt

@JsExport
enum class Foo {
    A,
    B;

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
    };

    val foo = ordinal

    fun bar(value: String) = value

    fun bay() = name
}

// FILE: test.js
function box() {
    if (this["export-enum-class"].Foo.A !== this["export-enum-class"].Foo.A) return "fail1"
    if (this["export-enum-class"].Foo.B !== this["export-enum-class"].Foo.B) return "fail2"

    if (this["export-enum-class"].Foo.Companion.baz !== "baz") return "fail3"

    if (this["export-enum-class"].Foo.A.foo !== 0) return "fail4"
    if (this["export-enum-class"].Foo.B.foo !== 1) return "fail5"

    if (this["export-enum-class"].Foo.A.bar("A") !== "A") return "fail6"
    if (this["export-enum-class"].Foo.B.bar("B") !== "B") return "fail7"

    if (this["export-enum-class"].Foo.A.bay() !== "A") return "fail8"
    if (this["export-enum-class"].Foo.B.bay() !== "B") return "fail9"

    if (this["export-enum-class"].Bar.A.foo !== 0) return "fail10"
    if (this["export-enum-class"].Bar.B.foo !== 1) return "fail11"

    if (this["export-enum-class"].Bar.A.bar("A") !== "A") return "fail12"
    if (this["export-enum-class"].Bar.B.bar("B") !== "B") return "fail13"

    if (this["export-enum-class"].Bar.A.bay() !== "A") return "fail14"
    if (this["export-enum-class"].Bar.B.bay() !== "B") return "fail15"

    if (this["export-enum-class"].Bar.B.constructor.prototype.hasOwnProperty('baz')) return "fail16"

    return "OK"
}