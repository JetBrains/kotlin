package foo

native
trait Chrome {
    val extension: Extension
}

native
trait Extension {
    val lastError: LastError?
}

native
trait LastError {
    val message: String
}

native
val chrome: Chrome = noImpl

fun box(): Boolean {
    val lastError = chrome.extension.lastError?.message
    return lastError == null
}