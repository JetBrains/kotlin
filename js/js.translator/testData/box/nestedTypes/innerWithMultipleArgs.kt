// EXPECTED_REACHABLE_NODES: 1286
// WITH_STDLIB

package foo

open class NotExportedParent(val a: Int, val b: Int) {
    inner class Inner(val c: Int, val d: Int) {
        fun foo() = a + b + c + d
    }

    inner class WithVararg(vararg val values: Int) {
        fun foo() = a + b + values.sum()
    }
}

@JsExport
open class ExportedParent(val a: Int, val b: Int) {
    inner class Inner(val c: Int, val d: Int) {
        fun foo() = a + b + c + d
    }

    inner class WithVararg(vararg val values: Int) {
        fun foo() = a + b + values.sum()
    }
}

fun box(): String {
    val notExportedParent = NotExportedParent(1, 2)
    val notExportedInner = notExportedParent.Inner(3, 4)
    val notExportedInnerWithVararg = notExportedParent.WithVararg(3, 4)

    if (notExportedInner.foo() != 10) return "Failed: something wrong with multiple arguments inside not-exported inner class primary constructor"
    if (notExportedInnerWithVararg.foo() != 10) return "Failed: something wrong with vararg arguments inside not-exported inner class primary constructor"

    val exportedParent = ExportedParent(1, 2)
    val exportedInner = exportedParent.Inner(3, 4)
    val exportedInnerWithVararg = exportedParent.WithVararg(3, 4)

    if (exportedInner.foo() != 10) return "Failed: something wrong with multiple arguments inside exported inner class primary constructor"
    if (exportedInnerWithVararg.foo() != 10) return "Failed: something wrong with vararg arguments inside exported inner class primary constructor"

    return "OK"
}

