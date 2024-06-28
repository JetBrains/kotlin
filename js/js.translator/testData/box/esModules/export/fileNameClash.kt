// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES

// MODULE: name_clash
// FILE: test1/lib.kt
package foo

@JsExport
open class A<T> {
    open fun foo(value: T): T = value
}

// FILE: test2/lib.kt
package foo

@JsExport
class B: A<String>() {
    override fun foo(value: String): String = value
}

// FILE: entry.mjs
// ENTRY_ES_MODULE
import { A, B } from "./fileNameClash-name_clash_v5.mjs";

export function box() {
    var a = new A()
    var aFoo = a.foo("ok")
    if (aFoo != "ok") return "fail 1"

    var b = new B()
    var bFoo = b.foo("ok")
    if (bFoo != "ok") return "fail 2"

    return "OK"
}
