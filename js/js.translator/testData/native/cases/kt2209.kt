package foo

@native
interface Chrome {
    val extension: Extension
}

@native
interface Extension {
    val lastError: LastError?
}

@native
interface LastError {
    val message: String
}

@native
val chrome: Chrome = noImpl

fun box(): Boolean {
    val lastError = chrome.extension.lastError?.message
    return lastError == null
}