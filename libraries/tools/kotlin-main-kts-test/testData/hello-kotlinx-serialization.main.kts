
@file:Repository("https://jcenter.bintray.com/")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0-1.4.0-dev-5730", options = ["transitive=false"])

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class User(val firstName: String, val lastName: String)

val json = Json(JsonConfiguration.Stable)

val jsonData = json.stringify(User.serializer(), User("James", "Bond"))
println(jsonData)

val obj = json.parse(User.serializer(), """{"firstName":"James", "lastName":"Bond"}""")
println(obj)

