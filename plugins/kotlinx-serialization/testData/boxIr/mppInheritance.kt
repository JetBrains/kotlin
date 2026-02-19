// IGNORE_BACKEND_K1: JVM_IR
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

import kotlinx.serialization.*

@Serializable
public abstract class Something {
    public val eventFlow: List<String> get() = error("")
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: jvm.kt

fun intermediate() {}

// MODULE: jvm2(jvm)()()
// TARGET_PLATFORM: JVM
// FILE: main.kt

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlin.test.assertEquals

@Serializable
data class Impl(var kek: String): Something()

fun assertImpl() {
    val impl = Impl("kek")
    val json = Json.encodeToString(impl)
    assertEquals("""{"kek":"kek"}""", json)
    val decoded = Json.decodeFromString(Impl.serializer(), json)
    assertEquals(impl, decoded)
}

fun box(): String {
    try {
        assertImpl()
        return "OK"
    } catch (e: Throwable) {
        e.printStackTrace()
        return e.message!!
    }
}
