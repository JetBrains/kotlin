// RUN_PLAIN_BOX_FUNCTION
// ISSUE: KT-86934

// MODULE: lib
// FILE: lib.kt

@JsExport
interface Test {
    suspend fun func(x: Int): String
}

@JsExport
open class Base {
    open suspend fun func(x: Int): String = "fail$x"
}

@JsExport
suspend fun main(): String {
    // Anonymous object implementing an exported interface (virtual bridge path + argument forwarding)
    val viaInterface: Test = object : Test {
        override suspend fun func(x: Int): String = "OK$x"
    }

    // Anonymous object extending an open exported class (non-interface implementor bridge path + argument forwarding)
    val viaClass: Base = object : Base() {
        override suspend fun func(x: Int): String = "OK${x + 1}"
    }

    // Anonymous object nested inside another anonymous object's suspend method (deeper nesting)
    val nested = object : Test {
        override suspend fun func(x: Int): String {
            val inner = object : Test {
                override suspend fun func(x: Int): String = "OK${x + 2}"
            }
            return inner.func(x)
        }
    }

    val r1 = viaInterface.func(42)
    val r2 = viaClass.func(42)
    val r3 = nested.func(42)

    return if (r1 == "OK42" && r2 == "OK43" && r3 == "OK44") "OK" else "fail: $r1 $r2 $r3"
}

// FILE: main.js
async function box() {
    var main = this.lib.main;

    return await main()
}
