package foo

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface User {
    var name: String
    val age: Int
}

@JsPlainObject
external interface ExtendedUser : User {
    val email: String
}


fun box(): String {
    val user = ExtendedUser(name = "Name", age = 10, email = "test@test")

    if (user.name != "Name") return "Fail: problem with `name` property"
    if (user.age != 10) return "Fail: problem with `age` property"
    if (user.email != "test@test") return "Fail: problem with `email` property"

    val json = js("JSON.stringify(user)")
    if (json != "{\"age\":10,\"name\":\"Name\",\"email\":\"test@test\"}") return "Fail: got the next json: $json"

    val simpleUser = User(name = "Name", age = 10)

    if (simpleUser.name != "Name") return "Fail: problem with `name` property"
    if (simpleUser.age != 10) return "Fail: problem with `age` property"

    val anotherJson = js("JSON.stringify(simpleUser)")
    if (anotherJson != "{\"age\":10,\"name\":\"Name\"}") return "Fail: got the next json: $anotherJson"

    return "OK"
}