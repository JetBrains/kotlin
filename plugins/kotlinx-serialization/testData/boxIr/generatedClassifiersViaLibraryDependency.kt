// ISSUE: KT-55759
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

import kotlinx.serialization.*

@Serializable
class Test

// MODULE: jvm(lib)()()
// FILE: main.kt

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

fun box(): String {
    return if (Test.serializer().descriptor.toString() == "Test()") "OK" else "FAIL"
}
