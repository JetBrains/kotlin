
@file:Repository("https://jcenter.bintray.com/")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0-1.4.0-rc-95", options = ["transitive=false"])

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class User(val firstName: String, val lastName: String)

val json = Json(JsonConfiguration.Stable)

val jsonData = json.encodeToString(User.serializer(), User("James", "Bond"))
println(jsonData)

val obj = json.decodeFromString(User.serializer(), """{"firstName":"James", "lastName":"Bond"}""")
println(obj)

