// MODULE: lib
// FILE: lib.kt
package foo

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface User {
    var name: String
    val age: Int
}

@JsPlainObject
external interface Admin {
    var email: String
    val level: Int
}

// MODULE: main(lib)
// FILE: main.kt
package foo

fun box(): String {
    val user = User(name = "Name", age = 10)

    if (user.name != "Name") return "Fail: problem with `name` property"
    if (user.age != 10) return "Fail: problem with `age` property"

    val userJson = js("JSON.stringify(user)")
    if (userJson != "{\"age\":10,\"name\":\"Name\"}") return "Fail: got the next json: $userJson"

    val admin = Admin(email = "admin@admin.com", level = 0)

    if (admin.email != "admin@admin.com") return "Fail: problem with `email` property"
    if (admin.level != 0) return "Fail: problem with `level` property"

    val adminJson = js("JSON.stringify(admin)")
    if (adminJson != "{\"email\":\"admin@admin.com\",\"level\":0}") return "Fail: got the next json: $adminJson"

    return "OK"
}
