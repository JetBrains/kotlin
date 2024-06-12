// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE REASON: multimodule MPP tests are not supported for K1
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

package a

//@OptIn(kotlin.ExperimentalMultiplatform::class)
//@OptionalExpectation
//@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.TYPE)
//expect annotation class MySerializable()

//@OptIn(kotlin.ExperimentalMultiplatform::class)
//@OptionalExpectation
//@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.TYPE)
//expect annotation class Serializable()
//
//@Serializable
//class Some

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

package a

//@kotlinx.serialization.MetaSerializable
//actual annotation class Serializable

typealias Serializable = kotlinx.serialization.Serializable

@Serializable
class Some

fun box(): String = if (Some.serializer().descriptor.toString() != "") "OK" else "fail"
