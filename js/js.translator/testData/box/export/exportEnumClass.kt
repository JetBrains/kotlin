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

    fun bar() = name

    companion object {
        val baz = "baz"
    }
}

// FILE: test.js
function box() {
    if (this["export-enum-class"].Foo.A !== this["export-enum-class"].Foo.A) return "fail1"
    if (this["export-enum-class"].Foo.B !== this["export-enum-class"].Foo.B) return "fail2"

    if (this["export-enum-class"].Foo.Companion.baz !== "baz") return "fail3"

    if (this["export-enum-class"].Foo.A.foo !== 0) return "fail4"
    if (this["export-enum-class"].Foo.B.foo !== 1) return "fail5"

    if (this["export-enum-class"].Foo.A.bar() !== "A") return "fail6"
    if (this["export-enum-class"].Foo.B.bar() !== "B") return "fail7"
}