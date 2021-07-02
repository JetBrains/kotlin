package serialization

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Data(val number: Int, val english: String, val germany:String)

fun main() {
    Data(42, "forty two", "zweiundvierzig").let {
        println(Json.encodeToString(it))
    }
}

