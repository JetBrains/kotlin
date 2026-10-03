// LANGUAGE: +CompanionBlocks +CompanionExtensions

// MODULE: main
// FILE: companion-extension-direct-export.kt

class WasmCompanionExtensionCarrier {
    companion {
        val prefix = "O"
    }
}

@JsExport
companion fun WasmCompanionExtensionCarrier.extensionAppend(value: String): String =
    WasmCompanionExtensionCarrier.prefix + value
