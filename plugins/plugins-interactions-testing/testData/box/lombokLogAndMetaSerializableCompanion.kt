// ENABLE_LOMBOK
// ENABLE_SERIALIZATION
// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-62194, KT-90321

@file:OptIn(ExperimentalSerializationApi::class)

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable
import kotlinx.serialization.json.Json
import lombok.extern.java.Log

@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class MySerializable

@Log
@MySerializable
class Sample(val name: String) {
    //fun info() = log.info(name)
}

fun box(): String {
    //Sample("x").info()
    val json = Json.encodeToString(Sample.serializer(), Sample("x"))
    return if (json == """{"name":"x"}""") "OK" else json
}
