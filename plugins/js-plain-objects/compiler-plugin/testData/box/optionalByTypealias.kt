package foo

import kotlinx.js.JsPlainObject

typealias OptionalString = String?

@JsPlainObject
external interface User {
    val email: OptionalString
}

fun box(): String {
    val user = User()

    val json = js("JSON.stringify(user)")
    if (json != "{}") return "Fail: got the next json: $json"

    val withEmail = User(email = "test@test")

    if (withEmail.email != "test@test") return "Fail: problem with emailed `email` property"

    val jsonWithEmail = js("JSON.stringify(withEmail)")
    if (jsonWithEmail != "{\"email\":\"test@test\"}") return "Fail: got the next emailed json: $jsonWithEmail"

    return "OK"
}