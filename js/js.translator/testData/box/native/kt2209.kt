// EXPECTED_REACHABLE_NODES: 1281
// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: LAZY_INIT_PROPERTIES
package foo

external interface Chrome {
    val extension: Extension
}

external interface Extension {
    val lastError: LastError?
}

external interface LastError {
    val message: String
}

external val chrome: Chrome = definedExternally

fun box(): String {
    val lastError = chrome.extension.lastError?.message
    return if (lastError == null) "OK" else "fail"
}