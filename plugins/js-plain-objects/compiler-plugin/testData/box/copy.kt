package foo

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface User {
    var name: String
    val age: Int
}

fun box(): String {
    val user = User(name = "Name", age = 10)
    val copy = User.copy(user, age = 11)

    if (copy === user) return "Fail: mutation instead of immutable copy"

    val json = js("JSON.stringify(copy)")

    if (copy.name != "Name") return "Fail: problem with copied `name` property"
    if (copy.age != 11) return "Fail: problem with copied `age` property"

    if (json != "{\"name\":\"Name\",\"age\":11}") return "Fail: got the next json for the copy: $json"

    return "OK"
}