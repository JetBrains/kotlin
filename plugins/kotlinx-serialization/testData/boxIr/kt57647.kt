// TARGET_BACKEND: JVM_IR

// WITH_STDLIB
// IGNORE_DEXING

import kotlinx.serialization.*
import java.util.UUID

@Serializable
@JvmInline
value class Id(val id: @Contextual UUID) {
    companion object {
        fun random() = Id(UUID.randomUUID())
    }
}

fun pageMain () {
    val id: Id = Id.random()
    println(id)
}


fun box(): String {
    println(System.getProperty("java.version"))
    pageMain()
    return "OK"
}
