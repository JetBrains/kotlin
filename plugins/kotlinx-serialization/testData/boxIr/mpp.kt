// IGNORE_BACKEND_K1: JVM_IR
// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

import kotlinx.serialization.*

@Serializable
class Bar<T>(val t: T)

@Serializable
class Wrapper(val b: Bar<String>)

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*

fun box(): String {
    return if (Wrapper.serializer().descriptor.elementDescriptors.first().toString() == "Bar(t: kotlin.String)") "OK" else "FAIL"
}