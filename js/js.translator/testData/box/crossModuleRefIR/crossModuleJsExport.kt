// IGNORE_BACKEND: JS
// SPLIT_PER_MODULE
// RUN_PLAIN_BOX_FUNCTION
// EXPECTED_REACHABLE_NODES: 1316

// MODULE: lib0
// FILE: lib0.kt

@JsExport
class Dep {
    fun bee() = "beedep"
}

// MODULE: lib1
// FILE: lib1.kt

@JsExport
fun O(): String = "O"

// MODULE: lib2(lib1, lib0)
// FILE: file.kt
@file:JsExport


fun K(): String = "K"
fun f2f(): Int = 42

// FILE: decl.kt

@JsExport
fun fooFun(): Int = 53

@JsExport
class CCC {
    fun bux() = 64
}

@JsExport
fun doCCC() = CCC()

@JsExport
val prop: String
    get() = "kek"

@JsExport
fun dep() = Dep()

// MODULE: main(lib1, lib2)
// FILE: main.kt

@JsExport
fun test() = O() + K()

// FILE: test.js

function box() {

    if (kotlin_lib2.f2f() != 42) return "fail 42";

    if (kotlin_lib2.fooFun() != 53) return "fail fooFun";

    var c = kotlin_lib2.doCCC();

    var CCC = kotlin_lib2.CCC

    if (c.bux() != 64) return "fail CCC.bux"

    if (!(c instanceof CCC)) return "fail instanceof CCC"

    var cc = new CCC();

    if (cc.bux() != 64) return "fail new CCC()";

    if (kotlin_lib2.prop != "kek") return "fail prop";

    var dex = kotlin_lib2.dep();

    if (typeof dex !== "object") return "fail: " + dex;

    if (dex.bee() != "beedep") return "fail beedep";

    if (main.test() !== "OK") return "fail 1";

    return kotlin_lib1.O() + kotlin_lib2.K();
}
