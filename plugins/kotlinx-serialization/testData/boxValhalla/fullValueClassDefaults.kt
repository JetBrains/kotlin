// LANGUAGE: +FullValueClasses

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
value class Range(val start: Int, val end: Int = start + 1, val name: String = "r$start")

@Serializable
value class Skipping(val start: Int, @Transient val step: Int = 7, val end: Int = start + step)

fun box(): String {
    val range = Json.decodeFromString<Range>("""{"start":1}""")
    if (range != Range(1, 2, "r1")) return "decoded: $range"
    val encoded = Json.encodeToString(Range(1))
    if (encoded != """{"start":1}""") return "encoded: $encoded"
    val full = Json.encodeToString(Range(1, 5, "x"))
    if (full != """{"start":1,"end":5,"name":"x"}""") return "encoded: $full"
    val skipping = Json.decodeFromString<Skipping>("""{"start":1}""")
    if (skipping != Skipping(1, 7, 8)) return "decoded: $skipping"
    val encodedSkipping = Json.encodeToString(Skipping(1, 2, 5))
    if (encodedSkipping != """{"start":1,"end":5}""") return "encoded: $encodedSkipping"
    return "OK"
}
