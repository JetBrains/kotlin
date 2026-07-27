// RUN_PLAIN_BOX_FUNCTION
// ISSUE: KT-86934

// MODULE: lib
// FILE: lib.kt

@JsExport
open class Base {
    open suspend fun func(): String = "O"
}

@JsExport
suspend fun main(): String {
    val test = object : Base() {
        override suspend fun func(): String = super.func() + "K"
    }

    return test.func()
}

// FILE: main.js
async function box() {
    return await this.lib.main()
}
