// RUN_PLAIN_BOX_FUNCTION
// ISSUE: KT-86934

// MODULE: lib
// FILE: lib.kt

@JsExport
open class Helper {
    open suspend fun value(): String = "fail"
}

@JsExport
interface Test {
    suspend fun func(): String
}

@JsExport
suspend fun run(helper: Helper): String {
    val test = object : Test {
        override suspend fun func(): String = "O" + helper.value()
    }

    return test.func()
}

// FILE: main.js
async function box() {
    const Helper = this.lib.Helper

    class JsHelper extends Helper {
        async value() {
            return "K"
        }
    }

    return await this.lib.run(new JsHelper())
}
