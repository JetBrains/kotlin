// KT-68943

package foo

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface BaseUser<out T> {
   val name: T
}

@JsPlainObject
external interface User<T> : BaseUser<T> {
    override val name: T
    val age: Int
}

fun assertUser(user: User<String>, name: String, age: Int): String {
    if (user.name != name) return "Fail: problem with `name` property"
    if (user.age != age) return "Fail: problem with `age` property"

    val json = js("JSON.stringify(user)")
    if (json != "{\"name\":\"$name\",\"age\":$age}") return "Fail: got the next json: $json"

    return "OK"
}

fun box(): String {
    val user = User(name = "Name", age = 10)

    assertUser(user, name = "Name", age = 10).let {
        if (it != "OK") return it
    }

    val userAsBaseUserCopy = User.copy(user, name = "New one")

    assertUser(userAsBaseUserCopy, name = "New one", age = 10).let {
        if (it != "OK") return it
    }

    return "OK"
}