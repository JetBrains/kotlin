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

fun box(): String {
    val user = User(name = "Name", age = 10)

    if (user.name != "Name") return "Fail: problem with `name` property"
    if (user.age != 10) return "Fail: problem with `age` property"

    val json = js("JSON.stringify(user)")
    if (json != "{\"name\":\"Name\",\"age\":10}") return "Fail: got the next json: $json"

    val userAsBaseUser: BaseUser<*> = user

    if (userAsBaseUser.name != "Name") return "Fail: problem with `name` property on BaseUser"

    val userAsBaseUserCopy = userAsBaseUser.copy(name = "New one")
    if (userAsBaseUserCopy.name != "New one") return "Fail: problem with copied `name` property on BaseUser"

    val userAsBaseUserCopyJson = js("JSON.stringify(userAsBaseUserCopy)")
    if (userAsBaseUserCopyJson != "{\"name\":\"New one\",\"age\":10}") return "Fail: got the next json: $userAsBaseUserCopyJson"

    return "OK"
}