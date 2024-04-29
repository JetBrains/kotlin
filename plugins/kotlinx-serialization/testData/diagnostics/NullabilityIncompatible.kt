// FIR_IDENTICAL
// WITH_STDLIB

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

// FILE: test.kt
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable(NopeNullableSerializer::class)
class Nope {}

class NopeNullableSerializer: KSerializer<Nope?> {
    override val descriptor: SerialDescriptor get() = TODO()
    override fun deserialize(decoder: Decoder): Nope? = TODO()
    override fun serialize(encoder: Encoder, value: Nope?) = TODO()
}

@Serializer(forClass = FooKept::class)
object FooKeptSerializer

@Serializable
class Foo(val foo: <!SERIALIZER_NULLABILITY_INCOMPATIBLE("NopeNullableSerializer; Nope")!>Nope<!>)

@Serializable(FooKeptSerializer::class)
@KeepGeneratedSerializer
class FooKept(val foo: <!SERIALIZER_NULLABILITY_INCOMPATIBLE("NopeNullableSerializer; Nope")!>Nope<!>)
