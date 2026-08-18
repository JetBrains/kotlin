// RUN_PLAIN_BOX_FUNCTION
// ISSUE: KT-86934

// MODULE: lib
// FILE: lib.kt

@JsExport
interface Test {
    suspend fun func(x: Int = 42): String
}

@JsExport
open class Base {
    open suspend fun func(x: Int = 7): String = "fail$x"
}

@JsExport
suspend fun main(): String {
    val viaInterface: Test = object : Test {
        override suspend fun func(x: Int): String = "OK$x"
    }

    val viaClass: Base = object : Base() {
        override suspend fun func(x: Int): String = "OK$x"
    }

    val interfaceDefault = viaInterface.func()
    val interfaceExplicit = viaInterface.func(1)
    val classDefault = viaClass.func()
    val classExplicit = viaClass.func(2)

    return if (
        interfaceDefault == "OK42" &&
        interfaceExplicit == "OK1" &&
        classDefault == "OK7" &&
        classExplicit == "OK2"
    ) {
        "OK"
    } else {
        "fail: $interfaceDefault $interfaceExplicit $classDefault $classExplicit"
    }
}

// FILE: main.js
async function box() {
    return await this.lib.main()
}
