// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT

// FILE: annotation.kt
package kotlinx.serialization

import kotlin.annotation.*

/*
  Until the annotation is added to the serialization runtime,
  we have to create an annotation with that name in the project itself
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KeepGeneratedSerializer

// FILE: main.kt
import kotlinx.serialization.*

@Serializable
class C(val values: IntArray) // OK

@Serializable
class B(val values: Array<String>) // OK

@Serializable
class A<T>(val values: <!GENERIC_ARRAY_ELEMENT_NOT_SUPPORTED!>Array<T><!>)

@Serializer(forClass = AKept::class)
class AKeptSerializer(val serializer: KSerializer<*>)

@Serializable(AKeptSerializer::class)
@KeepGeneratedSerializer
class AKept<T>(val values: <!GENERIC_ARRAY_ELEMENT_NOT_SUPPORTED!>Array<T><!>)
