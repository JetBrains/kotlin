package org.jetbrains.dokka.links

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * [DRI] stands for DokkaResourceIdentifier
 */
data class DRI(
    val packageName: String? = null,
    val classNames: String? = null,
    val callable: Callable? = null,
    val target: DriTarget = PointingToDeclaration,
    val extra: String? = null
) {
    override fun toString(): String =
        "${packageName.orEmpty()}/${classNames.orEmpty()}/${callable?.name.orEmpty()}/${callable?.signature()
            .orEmpty()}/$target/${extra.orEmpty()}"

    companion object {
        val topLevel = DRI()

    }
}

object EnumEntryDRIExtra: DRIExtraProperty<EnumEntryDRIExtra>()

abstract class DRIExtraProperty<T> {
    val key: String = this::class.qualifiedName
        ?: (this.javaClass.let { it.`package`.name + "." + it.simpleName.ifEmpty { "anonymous" } })
}


class DRIExtraContainer(val data: String? = null) {
    val map: MutableMap<String, Any> = if (data != null) OBJECT_MAPPER.readValue(data) else mutableMapOf()
    inline operator fun <reified T> get(prop: DRIExtraProperty<T>): T? =
        map[prop.key]?.let { prop as? T }

    inline operator fun <reified T> set(prop: DRIExtraProperty<T>, value: T) =
        value.also { map[prop.key] = it as Any }

    fun encode(): String = OBJECT_MAPPER.writeValueAsString(map)

    private companion object {
        private val OBJECT_MAPPER = ObjectMapper()
    }
}

val DriOfUnit = DRI("kotlin", "Unit")
val DriOfAny = DRI("kotlin", "Any")

fun DRI.withClass(name: String) = copy(classNames = if (classNames.isNullOrBlank()) name else "$classNames.$name")

fun DRI.withTargetToDeclaration() = copy(target = PointingToDeclaration)

fun DRI.withEnumEntryExtra() = copy(
    extra = DRIExtraContainer(this.extra).also { it[EnumEntryDRIExtra] = EnumEntryDRIExtra }.encode()
)

val DRI.parent: DRI
    get() = when {
        extra != null -> when {
            DRIExtraContainer(extra)[EnumEntryDRIExtra] != null -> copy(
                classNames = classNames?.substringBeforeLast(".", "")?.takeIf { it.isNotBlank() },
                extra = null
            )
            else -> copy(extra = null)
        }
        target != PointingToDeclaration -> copy(target = PointingToDeclaration)
        callable != null -> copy(callable = null)
        classNames != null -> copy(classNames = classNames.substringBeforeLast(".", "").takeIf { it.isNotBlank() })
        else -> DRI.topLevel
    }

val DRI.sureClassNames
    get() = classNames ?: throw IllegalStateException("Malformed DRI. It requires classNames in this context.")

data class Callable(
    val name: String,
    val receiver: TypeReference? = null,
    val params: List<TypeReference>
) {
    fun signature() = "${receiver?.toString().orEmpty()}#${params.joinToString("#")}"

    companion object
}

@JsonTypeInfo(use = CLASS)
sealed class TypeReference {
    companion object
}

data class JavaClassReference(val name: String) : TypeReference() {
    override fun toString(): String = name
}

data class TypeParam(val bounds: List<TypeReference>) : TypeReference()

data class TypeConstructor(
    val fullyQualifiedName: String,
    val params: List<TypeReference>
) : TypeReference() {
    override fun toString() = fullyQualifiedName +
            (if (params.isNotEmpty()) "[${params.joinToString(",")}]" else "")
}

data class RecursiveType(val rank: Int): TypeReference() {
    override fun toString() = "^".repeat(rank + 1)
}

data class Nullable(val wrapped: TypeReference) : TypeReference() {
    override fun toString() = "$wrapped?"
}

object StarProjection : TypeReference() {
    override fun toString() = "*"
}

@JsonTypeInfo(use = CLASS)
sealed class DriTarget {
    override fun toString(): String = this.javaClass.simpleName

    companion object
}

data class PointingToGenericParameters(val parameterIndex: Int) : DriTarget() {
    override fun toString(): String = "PointingToGenericParameters($parameterIndex)"
}

object PointingToDeclaration : DriTarget()

data class PointingToCallableParameters(val parameterIndex: Int) : DriTarget() {
    override fun toString(): String = "PointingToCallableParameters($parameterIndex)"
}

fun DriTarget.nextTarget(): DriTarget = when (this) {
    is PointingToGenericParameters -> PointingToGenericParameters(this.parameterIndex + 1)
    is PointingToCallableParameters -> PointingToCallableParameters(this.parameterIndex + 1)
    else -> this
}
