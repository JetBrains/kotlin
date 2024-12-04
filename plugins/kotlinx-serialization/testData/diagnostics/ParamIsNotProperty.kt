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
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

<!PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY!>@Serializable<!>
class Test(val someData: String, cantBeDeserialized: Int)

object TestKeptSerializer: KSerializer<TestKept> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TestKeptDesc", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): TestKept = TODO()
    override fun serialize(encoder: Encoder, value: TestKept) = TODO()
}

<!PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY!>@Serializable(TestKeptSerializer::class)<!>
@KeepGeneratedSerializer
class TestKept(val someData: String, cantBeDeserialized: Int)