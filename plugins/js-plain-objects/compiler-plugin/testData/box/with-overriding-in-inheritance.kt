package foo

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface User {
    val name: Any
    val age: Int
}

@JsPlainObject
external interface ExtendedUser : User {
    override val name: String
    val email: String
}

fun box(): String {
    val user = ExtendedUser(name = "Name", age = 10, email = "test@test")

    if (user.name != "Name") return "Fail: problem with `name` property"
    if (user.age != 10) return "Fail: problem with `age` property"
    if (user.email != "test@test") return "Fail: problem with `email` property"

    val json = js("JSON.stringify(user)")
    if (json != "{\"name\":\"Name\",\"age\":10,\"email\":\"test@test\"}") return "Fail: got the next json: $json"

    val copy = User.copy(user)
    val copiedJson = js("JSON.stringify(copy)")
    if (copiedJson != "{\"name\":\"Name\",\"age\":10,\"email\":\"test@test\"}") return "Fail: got the next json: $copiedJson"

    val simpleUser = User(name = 42, age = 10)

    if (simpleUser.name != 42) return "Fail: problem with `name` property"
    if (simpleUser.age != 10) return "Fail: problem with `age` property"

    val anotherJson = js("JSON.stringify(simpleUser)")
    if (anotherJson != "{\"name\":42,\"age\":10}") return "Fail: got the next json: $anotherJson"

    val anotherCopy = User.copy(simpleUser)
    val anotherCopiedJson = js("JSON.stringify(anotherCopy)")
    if (anotherCopiedJson != "{\"name\":42,\"age\":10}") return "Fail: got the next json: $anotherCopiedJson"

    return "OK"
}