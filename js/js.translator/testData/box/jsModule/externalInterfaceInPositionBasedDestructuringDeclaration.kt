// JS_MODULE_KIND: COMMON_JS
// SKIP_DCE_DRIVEN
// LANGUAGE: +NameBasedDestructuring

external interface JsPerson {
    val id: Int
    val name: String
}

external val person: JsPerson

operator fun JsPerson.component1() = id
operator fun JsPerson.component2() = name

fun box(): String {
    val [first, second] = person
    if (first != 42 || second != "Gerda")
        return "Fail1: ${first} ${second}"

    [val fullId, var fullName] = person
    fullName += "!"
    if (fullId != 42 || fullName != "Gerda!")
        return "Fail2: ${fullId} ${fullName}"

    return "OK"
}
