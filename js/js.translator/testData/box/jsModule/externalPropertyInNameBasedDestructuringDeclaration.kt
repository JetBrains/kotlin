// JS_MODULE_KIND: COMMON_JS
// SKIP_DCE_DRIVEN
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm

external val jsId: Int

external val jsName: String

data class Person(val id: Int = jsId, val name: String = id.toString())

fun box(): String {
    val (name, id) = Person(jsId, jsName)
    if (id != 42 || name != "Gerda")
        return "Fail1: ${id} ${name}"

    (val fullId = id, var fullName = name) = Person(jsId, jsName)
    fullName += "!"
    if (fullId != 42 || fullName != "Gerda!")
        return "Fail2: ${fullId} ${fullName}"

    (val defaultId = id, val defaultName = name) = Person()
    if (defaultId != 42 || defaultName != "42")
        return "Fail3: ${defaultId} ${defaultName}"

    return "OK"
}
