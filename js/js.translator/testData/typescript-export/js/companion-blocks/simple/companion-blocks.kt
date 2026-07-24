// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// MODULE: JS_TESTS
// FILE: companion-blocks.kt

package foo

@JsExport
class ExportedWithCompanionBlock {
    companion {
        fun append(value: String = "K"): String {
            mutable = value
            return readOnly + mutable
        }

        val readOnly: String = "O"
        var mutable: String = ""
    }

    fun appendToInstance(value: String = "K"): String {
        instanceMutable = value
        return instanceReadOnly + instanceMutable
    }

    val instanceReadOnly: String = "I"
    var instanceMutable: String = ""
}

@JsExport
open class ExportedBase {
    companion {
        fun shared(): String = "base"

        fun baseOnly(): String = "baseOnly"
    }
}

@JsExport
class ExportedChild : ExportedBase() {
    companion {
        fun shared(): String = "child"

        fun childOnly(): String = "childOnly"
    }
}

@JsExport
interface ExportedInterfaceWithCompanion {
    companion {
        fun interfaceFun(): String = "interface"
    }
}
