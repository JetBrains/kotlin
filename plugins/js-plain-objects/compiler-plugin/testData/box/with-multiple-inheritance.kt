package foo

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface User {
    var name: String
    val age: Int
}

@JsPlainObject
external interface Role {
    val role: String
}

@JsPlainObject
external interface ExtendedUser : User, Role {
    val email: String
}

fun box(): String {
    val user = ExtendedUser(name = "Name", age = 10, email = "test@test", role = "Admin")

    if (user.name != "Name") return "Fail: problem with `name` property"
    if (user.age != 10) return "Fail: problem with `age` property"
    if (user.email != "test@test") return "Fail: problem with `email` property"
    if (user.role != "Admin") return "Fail: problem with `role` property"

    val json = js("JSON.stringify(user)")
    if (json != "{\"age\":10,\"name\":\"Name\",\"role\":\"Admin\",\"email\":\"test@test\"}") return "Fail: got the next json: $json"

    return "OK"
}