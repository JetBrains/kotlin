// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

@Serializable()
class Dto(
    val data: Map<Int, Int>
) : Map<Int, Int> by data

fun box(): String {
    val dto = Dto(mapOf(1 to 2))
    val s = Json.encodeToString(dto)
    if (s != """{"data":{"1":2}}""") return s
    val d = Json.decodeFromString<Dto>(s)
    if (d.size != 1) return "Delegation to Map failed"
    if (d.data != dto.data) return "Equals failed ${d.data}"
    return "OK"
}
