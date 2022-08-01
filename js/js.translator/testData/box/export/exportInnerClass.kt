// EXPECTED_REACHABLE_NODES: 1252
// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: export_inner_class
// FILE: lib.kt

@JsExport
class RegularParent(val value: String) {
    inner class RegularInner(val message: String, val anotherValue: Int) {
        fun getResult() = value + message
    }
}


@JsExport
class ParentForSecondary {
    inner class InnerWithSecondaryConstructor(val value: String) {
        @JsName("innerSuccess")
        constructor(): this("OK")
    }
}

@JsExport
class ParentWithSecondary(val value: String) {
    @JsName("createO")
    constructor(): this("O")

    inner class InnerWithSecondaryConstructor(val anotherValue: String) {
        @JsName("createK")
        constructor(): this("K")

        fun getResult() = value + anotherValue
    }
}



// FILE: test.js
function box() {
    var pckg = this["export_inner_class"]

    var regularParent = new pckg.RegularParent("O")
    var regularInner = new regularParent.RegularInner("K", 42)

    if (regularInner.anotherValue !== 42) return "Fail: second parameter of the RegularInner primary constructor was ignored"
    if (regularInner.getResult() !== "OK") return "Fail: something is going wrong with the outer this capturing logic"

    var parentForSecondary = new pckg.ParentForSecondary()
    var innerWithSecondary = new parentForSecondary.InnerWithSecondaryConstructor("OK")

    if (innerWithSecondary.value !== "OK") return "Fail: something is going wrong with primary constructor when a secondary one exists"

    var fromSecondary = parentForSecondary.InnerWithSecondaryConstructor.innerSuccess()

    if (fromSecondary.value !== "OK") return "Fail: something is going wrong with secondary constructor inside the inner class"

    var parentFromSecondary = pckg.ParentWithSecondary.createO()
    var innerFromSecondary = parentFromSecondary.InnerWithSecondaryConstructor.createK()

    if (innerFromSecondary.getResult() !== "OK") return "Fail: there is a problem when both parent and inner class have secondary constructors"

    return "OK"
}