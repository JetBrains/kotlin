// JS_MODULE_KIND: COMMON_JS
// SKIP_DCE_DRIVEN
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

external fun person(): dynamic

fun box(): String {
    val (name, id) = person()
    if (id != 42 || name != "Gerda")
        return "Fail1: ${id} ${name}"

    (val fullId = id, var fullName = name) = person()
    fullName += "!"
    if (fullId != 42 || fullName != "Gerda!")
        return "Fail2: ${fullId} ${fullName}"

    return "OK"
}
