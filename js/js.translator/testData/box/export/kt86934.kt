// RUN_PLAIN_BOX_FUNCTION
// ISSUE: KT-86934

// MODULE: lib
// FILE: lib.kt

@JsExport
interface Test {
    suspend fun func(): String
}

@JsExport
open class Base {
    open suspend fun func(): String = "fail"
}

@JsExport
suspend fun main(): String {
    // Anonymous object implementing an exported interface (virtual bridge path)
    val viaInterface = object : Test {
        override suspend fun func(): String = "OK"
    }

    // Anonymous object extending an open exported class (non-interface implementor bridge path)
    val viaClass = object : Base() {
        override suspend fun func(): String = "OK"
    }

    // Anonymous object nested inside another anonymous object's suspend method (deeper nesting)
    val nested = object : Test {
        override suspend fun func(): String {
            val inner = object : Test {
                override suspend fun func(): String = "OK"
            }
            return inner.func()
        }
    }

    val r1 = viaInterface.func()
    val r2 = viaClass.func()
    val r3 = nested.func()

    return if (r1 == "OK" && r2 == "OK" && r3 == "OK") "OK" else "fail: $r1 $r2 $r3"
}

// FILE: main.js
async function box() {
    var main = this.lib.main;

    return await main()
}
