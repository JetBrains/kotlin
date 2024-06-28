package foo

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface User {
    var name: String
    val age: Int
    val email: String?
}

fun box(): String {
    val user = User(name = "Name", age = 10)

    if (user.name != "Name") return "Fail: problem with `name` property"
    if (user.age != 10) return "Fail: problem with `age` property"

    val json = js("JSON.stringify(user)")
    if (json != "{\"age\":10,\"name\":\"Name\"}") return "Fail: got the next json: $json"

    val withEmail = User(name = "Name", age = 10, email = "test@test")

    if (withEmail.name != "Name") return "Fail: problem with emailed `name` property"
    if (withEmail.age != 10) return "Fail: problem with emailed `age` property"
    if (withEmail.email != "test@test") return "Fail: problem with emailed `email` property"

    val jsonWithEmail = js("JSON.stringify(withEmail)")
    if (jsonWithEmail != "{\"email\":\"test@test\",\"age\":10,\"name\":\"Name\"}") return "Fail: got the next emailed json: $jsonWithEmail"

    return "OK"
}