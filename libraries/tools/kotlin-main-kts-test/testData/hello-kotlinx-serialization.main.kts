
@file:Repository("https://jcenter.bintray.com/")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-runtime:1.0-M1-1.4.0-rc", options = ["transitive=false"])

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class User(val firstName: String, val lastName: String)

val jsonData = Json.encodeToString(User.serializer(), User("James", "Bond"))
println(jsonData)

val obj = Json.decodeFromString(User.serializer(), """{"firstName":"James", "lastName":"Bond"}""")
println(obj)

