// EXPECTED_REACHABLE_NODES: 1286
package foo

open class NotExportedParent(val o: String) {
    constructor(): this("O")

    inner class Inner(val k: String) {
        constructor(): this("K")
        fun foo() = o + k
    }
}

@JsExport
open class ExportedParent(val o: String) {
    @JsName("createO")
    constructor(): this("O")

    inner class Inner(val k: String) {
        @JsName("createK")
        constructor(): this("K")
        fun foo() = o + k
    }
}

fun box(): String {
    val notExportedParent = NotExportedParent("OO")

    if (notExportedParent.Inner("KK").foo() != "OOKK") return "Fail1: primary constructor capturing"
    if (notExportedParent.Inner().foo() != "OOK") return "Fail2: inner secondary constructor capturing"

    val notExportedParentDefault = NotExportedParent()

    if (notExportedParentDefault.Inner("KK").foo() != "OKK") return "Fail3: primary constructor capturing"
    if (notExportedParentDefault.Inner().foo() != "OK") return "Fail4: inner secondary constructor capturing"

    val exportedParent = ExportedParent("OO")

    if (exportedParent.Inner("KK").foo() != "OOKK") return "Fail5: exported primary constructor capturing"
    if (exportedParent.Inner().foo() != "OOK") return "Fail6: exported inner secondary constructor capturing"

    val exportedParentDefault = ExportedParent()

    if (exportedParentDefault.Inner("KK").foo() != "OKK") return "Fail7: exported primary constructor capturing"
    if (exportedParentDefault.Inner().foo() != "OK") return "Fail8: exported inner secondary constructor capturing"

    return "OK"
}

