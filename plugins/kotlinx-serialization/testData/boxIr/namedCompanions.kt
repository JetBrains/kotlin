// WITH_STDLIB
// WITH_REFLECT
// TARGET_BACKEND: JVM_IR

package kotlinx.serialization.internal

import kotlin.annotation.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.reflect.KClass

/*
  Until the annotation is added to the serialization runtime,
  we have to create an annotation with that name in the project itself
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class NamedCompanion


@Serializable
class Plain(val i: Int)

@Serializable
object Object


@Serializable
class WithStd(val i: Int) {
    companion object {
        var j: Int = 42
    }
}

@Serializable
class WithNamed(val i: Int) {
    companion object MyCompanion {
        var j: Int = 42
    }
}


object InterfaceWithCustomSerializer: ToDoSerializer<InterfaceWithCustom>("interface")
@Serializable(InterfaceWithCustomSerializer::class)
interface InterfaceWithCustom {
}


object NamedInterfaceWithCustomSerializer: ToDoSerializer<NamedInterfaceWithCustom>("interface.name")
@Serializable(NamedInterfaceWithCustomSerializer::class)
interface NamedInterfaceWithCustom {
    companion object MyCompanion {
        var j: Int = 42
    }
}

@Serializable
sealed interface SealedInterface {
}

@Serializable
sealed interface NamedSealedInterface {
    companion object MyCompanion {
        var j: Int = 42
    }
}

fun box(): String {
    if (Plain::class.annotations.any { it.annotationClass.simpleName == "NamedCompanion" }) {
        return "Annotation on Plain class"
    }
    if (Plain.Companion::class.annotations.any { it.annotationClass.simpleName == "NamedCompanion" }) {
        return "Annotation on Plain.Companion class"
    }

    if (Object::class.annotations.any { it.annotationClass.simpleName == "NamedCompanion" }) {
        return "Annotation on Object"
    }

    if (WithStd::class.annotations.any { it.annotationClass.simpleName == "NamedCompanion" }) {
        return "Annotation on WithStd class"
    }
    if (WithStd.Companion::class.annotations.any { it.annotationClass.simpleName == "NamedCompanion" }) {
        return "Annotation on WithStd.Companion class"
    }

    if (WithNamed::class.annotations.any { it.annotationClass.simpleName == "NamedCompanion" }) {
        return "Annotation on WithNamed class"
    }
    if (WithNamed.MyCompanion::class.annotations.none { it.annotationClass.simpleName == "NamedCompanion" }) {
        return "Missed annotation on WithNamed.MyCompanion class ${WithNamed.MyCompanion::class.annotations}"
    }

    if (InterfaceWithCustom::class.annotations.any { it.annotationClass.simpleName == "NamedCompanion" }) {
        return "Annotation on InterfaceWithCustom class"
    }
    if (NamedInterfaceWithCustom.MyCompanion::class.annotations.none { it.annotationClass.simpleName == "NamedCompanion" }) {
        return "Missed annotation on NamedInterfaceWithCustom.MyCompanion class ${WithNamed.MyCompanion::class.annotations}"
    }

    if (SealedInterface::class.annotations.any { it.annotationClass.simpleName == "NamedCompanion" }) {
        return "Annotation on SealedInterface class"
    }
    if (NamedSealedInterface.MyCompanion::class.annotations.none { it.annotationClass.simpleName == "NamedCompanion" }) {
        return "Missed annotation on NamedSealedInterface.MyCompanion class ${WithNamed.MyCompanion::class.annotations}"
    }

    return "OK"
}

abstract class ToDoSerializer<T: Any>(descriptorName: String): KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(descriptorName, PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): T = TODO()
    override fun serialize(encoder: Encoder, value: T) = TODO()
}
