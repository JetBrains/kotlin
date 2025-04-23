package foo

import kotlinx.js.JsPlainObject

external class Parent {
    @JsPlainObject
    interface User {
        var name: String
        val age: Int
    }
}

fun box(): String {
    val user = Parent.User(name = "Name", age = 10)

    if (user.name != "Name") return "Fail: problem with `name` property"
    if (user.age != 10) return "Fail: problem with `age` property"

    val json = js("JSON.stringify(user)")
    if (json != "{\"name\":\"Name\",\"age\":10}") return "Fail: got the next json: $json"

    return "OK"
}