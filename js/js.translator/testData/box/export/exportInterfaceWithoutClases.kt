// KT-60131
// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// SKIP_DCE_DRIVEN

// MODULE: export_interface
// FILE: lib.kt

@JsExport
interface BaseInterface {
    val prop: String
}

class Impl : BaseClass("foobar")

open class BaseClass(private val _prop: String) : BaseInterface {
    final override val prop: String get() = _prop
}

@JsExport
fun getImpl(): BaseInterface = Impl()

@JsExport
fun getBase(): BaseInterface = BaseClass("base")

// FILE: test.js
function box() {
    if (this["export_interface"].getBase().prop != "base") return "fail 1";
    if (this["export_interface"].getImpl().prop != "foobar") return "fail 2";

    return "OK"
}