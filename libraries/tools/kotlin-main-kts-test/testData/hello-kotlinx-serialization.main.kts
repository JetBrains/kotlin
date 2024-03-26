
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class User(val firstName: String, val lastName: String)

val jsonData = Json.encodeToString(User("James", "Bond"))
println(jsonData)

val obj = Json.decodeFromString<User>("""{"firstName":"James", "lastName":"Bond"}""")
println(obj)

