// WITH_STDLIB
// FIR_IDENTICAL

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
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

<!KEEP_SERIALIZER_ANNOTATION_USELESS!>@KeepGeneratedSerializer<!>
class NonSerializable(val i: Int)

@Serializable
<!KEEP_SERIALIZER_ANNOTATION_USELESS!>@KeepGeneratedSerializer<!>
class WithoutCustom(val i: Int)


@Serializable(AbstractSerializer::class)
<!KEEP_SERIALIZER_ANNOTATION_ON_POLYMORPHIC!>@KeepGeneratedSerializer<!>
abstract class Abstract(val i: Int)
@Serializer(forClass = Abstract::class)
object AbstractSerializer

@Serializable(SealedSerializer::class)
<!KEEP_SERIALIZER_ANNOTATION_ON_POLYMORPHIC!>@KeepGeneratedSerializer<!>
sealed class Sealed(val i: Int)
@Serializer(forClass = Sealed::class)
object SealedSerializer

@Serializable(InterfaceSerializer::class)
<!KEEP_SERIALIZER_ANNOTATION_ON_POLYMORPHIC!>@KeepGeneratedSerializer<!>
interface Interface
object InterfaceSerializer: ToDoSerializer<Interface>("Interface")

@Serializable(SealedInterfaceSerializer::class)
<!KEEP_SERIALIZER_ANNOTATION_ON_POLYMORPHIC!>@KeepGeneratedSerializer<!>
sealed interface SealedInterface
object SealedInterfaceSerializer: ToDoSerializer<SealedInterface>("SealedInterface")

@Serializable
@Polymorphic
<!KEEP_SERIALIZER_ANNOTATION_ON_POLYMORPHIC, KEEP_SERIALIZER_ANNOTATION_USELESS!>@KeepGeneratedSerializer<!>
open class ClassExplicitlyPolymorphic

@Serializable
@Polymorphic
<!KEEP_SERIALIZER_ANNOTATION_ON_POLYMORPHIC, KEEP_SERIALIZER_ANNOTATION_USELESS!>@KeepGeneratedSerializer<!>
sealed interface SealedInterfaceExplicitlyPolymorphic


abstract class ToDoSerializer<T>(name: String): KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)
    open override fun deserialize(decoder: Decoder): T = TODO()
    open override fun serialize(encoder: Encoder, value: T) = TODO()
}
