// KT-68943

package foo

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface Friends<E, T: Array<E>> {
   val items: T
}

@JsPlainObject
external interface User<T> {
    var name: T
    val age: Int
    val friends: Friends<String, Array<String>>
}

fun box(): String {
    val friends = Friends(arrayOf("Friend"))
    val user = User(name = "Name", age = 10, friends = friends)

    if (user.name != "Name") return "Fail: problem with `name` property"
    if (user.age != 10) return "Fail: problem with `age` property"
    if (user.friends.items[0] != "Friend") return "Fail: problem with `friends.items` property"

    val json = js("JSON.stringify(user)")
    if (json != "{\"friends\":{\"items\":[\"Friend\"]},\"age\":10,\"name\":\"Name\"}") return "Fail: got the next json: $json"

    return "OK"
}