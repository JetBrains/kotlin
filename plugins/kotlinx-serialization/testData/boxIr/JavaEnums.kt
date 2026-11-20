// WITH_STDLIB
// TARGET_BACKEND: JVM

// FILE: JavaEnum.java

import kotlinx.serialization.Serializable;

@Serializable
public enum JavaEnum {
    A, B
}

// FILE: main.kt

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class EnumHolder(val enum: JavaEnum)

fun box(): String {
    val json = Json.encodeToString(EnumHolder(JavaEnum.A))

    if (json != """{"enum":"A"}""") return "Illegal JSON: $json"
    return "OK"
}
