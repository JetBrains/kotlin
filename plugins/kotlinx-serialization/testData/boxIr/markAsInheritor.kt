// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

package a

import kotlinx.serialization.*

@Serializable
sealed class S

@Serializable
sealed interface I

sealed class NonSerializable

@Serializable
class A

@Serializable
class B: S()

@Serializable
class C: I

@Serializable
class D: NonSerializable()

@Serializable
class E: NonSerializable(), I

fun box(): String {
    // TODO: uncomment these tests when serialization runtime is up to 1.7.0
//    if (A.serializer().descriptor.isPartOfSealedHierarchy) return "A"
//    if (!B.serializer().descriptor.isPartOfSealedHierarchy) return "B"
//    if (!C.serializer().descriptor.isPartOfSealedHierarchy) return "C"
//    if (D.serializer().descriptor.isPartOfSealedHierarchy) return "D"
//    if (!E.serializer().descriptor.isPartOfSealedHierarchy) return "E"
    return "OK"
}
