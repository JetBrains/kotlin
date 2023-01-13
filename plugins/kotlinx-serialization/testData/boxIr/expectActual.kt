// IGNORE_BACKEND_K2: JVM_IR
// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// FILE: common.kt

package foo

import kotlinx.serialization.*

@Serializable
expect class Expected {
    companion object { fun factoryMethod(): Expected }
}

// FILE: jvm.kt

package foo

import kotlinx.serialization.*

@Serializable
actual class Expected {
    actual companion object { actual fun factoryMethod(): Expected = Expected() }
}


fun box(): String {
    val b = Expected.factoryMethod()
    if (b !is Expected) return "Incorrect factory method"
    val desc = Expected.serializer().descriptor
    if (desc.toString() != "foo.Expected()") return "Incorrect descriptor $desc"
    return "OK"
}