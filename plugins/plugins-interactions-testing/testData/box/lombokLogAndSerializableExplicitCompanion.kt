// ENABLE_LOMBOK
// ENABLE_SERIALIZATION
// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-90321

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lombok.extern.java.Log

@Log
@Serializable
class Sample(val name: String) {
    fun info() = log.info(name)
    companion object
}

fun box(): String {
    Sample("x").info()
    val json = Json.encodeToString(Sample.serializer(), Sample("x"))
    return if (json == """{"name":"x"}""") "OK" else json
}
