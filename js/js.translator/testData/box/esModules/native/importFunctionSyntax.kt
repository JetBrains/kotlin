// DONT_TARGET_EXACT_BACKEND: JS
// KJS_WITH_FULL_RUNTIME
// ES_MODULES

fun box(): String {
    if (js("import('hello')") !is kotlin.js.Promise<*>) return "fail1"
    if (!js("import.meta").url) return "fail2"

    return "OK"
}