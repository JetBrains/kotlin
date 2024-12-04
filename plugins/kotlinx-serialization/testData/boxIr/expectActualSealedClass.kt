// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE REASON: multimodule MPP tests are not supported for K1
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

import kotlinx.serialization.*

@Serializable
expect sealed class Some

fun s(): KSerializer<Some> = Some.Companion.serializer()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

import kotlinx.serialization.*

@Serializable
actual sealed class Some

fun box(): String = if (s().descriptor.toString() == Some.serializer().descriptor.toString()) "OK" else "fail"
