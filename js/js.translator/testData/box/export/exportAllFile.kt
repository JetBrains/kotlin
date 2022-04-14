// EXPECTED_REACHABLE_NODES: 1252
// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: export_all_file
// FILE: lib.kt
@file:JsExport

abstract class A {
    abstract fun foo(k: String): String

    abstract val bar: String

    abstract val baz: String

    abstract var bay: String

    abstract var bat: String
}

open class B : A() {
    override fun foo(k: String): String {
        return "O" + k
    }

    override val bar: String = "bar"

    override val baz: String
        get() = "baz"

    override var bay: String = "bay"

    private var _bat: String = "bat"

    override var bat: String
        get() = _bat
        set(value) {
            _bat = value
        }
}

interface I {
    val gap: String

    val hap: String

    var baz: String

    var bay: String

    fun foo(): String
}

open class C : I {
    override val gap: String = "gap"

    override val hap: String
        get() = "hap"

    override var bay: String = "bay"

    private var _baz = "baz"

    override var baz: String
        get() = _baz
        set(value) {
            _baz = value
        }

    val koo: String = "koo"

    override fun foo(): String {
        return "foo"
    }

    fun foi() = "foi"
}

fun topFoo(a: A): String {
    if (a.bar != "bar3") return "fail"
    if (a.baz != "baz3") return "fail"
    return "OK"
}

fun topBar(c: C): String {
    if (c.bay != "bay3") return "fail"
    if (c.baz != "baz3") return "fail"
    return "OK"
}

// FILE: test.js

function box() {
    var exportObject = this["export_all_file"]

    function H() {
    }

    H.prototype = Object.create(exportObject.B.prototype);
    H.prototype.constructor = H;
    Object.defineProperty(H.prototype, "bar", {
        get: function() {
        return "bar3"
    }
    })

    Object.defineProperty(H.prototype, "baz", {
        get: function() {
        return "baz3"
    }
    })

    function J() {
    }

    J.prototype = Object.create(exportObject.C.prototype);
    J.prototype.constructor = J;
    Object.defineProperty(J.prototype, "bay", {
        get: function() {
        return "bay3"
    }
    })

    Object.defineProperty(J.prototype, "baz", {
        get: function() {
        return "baz3"
    }
    })

    var b = new exportObject.B()
    if (b.foo("K") != "OK") return "fail 1";
    if (b.bar != "bar") return "fail 2"
    if (b.baz != "baz") return "fail 3"
    if (b.bay != "bay") return "fail 4"
    b.bay = "bay2"
    if (b.bay != "bay2") return "fail 5"
    if (b.bat != "bat") return "fail6"
    b.bat = "bat2"
    if (b.bat != "bat2") return "fail7"

    var c = new exportObject.C()
    if (c.gap != "gap") return "fail 8"
    if (c.hap != "hap") return "fail 9"
    if (c.bay != "bay") return "fail 10"
    c.bay = c.bay + "2"
    if (c.bay != "bay2") return "fail 11"
    if (c.baz != "baz") return "fail 12"
    c.baz = c.baz + "2"
    if (c.baz != "baz2") return "fail 13"
    if (c.foo() != "foo") return "fail 14"
    if (c.koo != "koo") return "fail 15"
    if (c.foi() != "foi") return "fail 16"

    if (exportObject.topFoo(new H()) != "OK") return "fail 17"
    if (exportObject.topBar(new J()) != "OK") return "fail 18"

    return "OK"
}