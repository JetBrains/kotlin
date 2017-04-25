// EXPECTED_REACHABLE_NODES: 488
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