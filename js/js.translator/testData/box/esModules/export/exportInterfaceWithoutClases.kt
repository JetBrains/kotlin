// KT-60131
// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES

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

// FILE: main.mjs
// ENTRY_ES_MODULE
import { getBase, getImpl } from "./exportInterfaceWithoutClases-export_interface_v5.mjs"

export function box() {
    if (getBase().prop != "base") return "fail 1";
    if (getImpl().prop != "foobar") return "fail 2";

    return "OK"
}