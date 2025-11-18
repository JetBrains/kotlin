/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.links

import org.jetbrains.dokka.ExperimentalDokkaApi
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.collections.List

/**
 * [DRI] stands for DokkaResourceIdentifier
 */
public data class DRI(
    val packageName: String? = null,
    val classNames: String? = null,
    val callable: Callable? = null,
    val target: DriTarget = PointingToDeclaration,
    val extra: String? = null
) {
    override fun toString(): String =
        "${packageName.orEmpty()}/${classNames.orEmpty()}/${callable?.name.orEmpty()}/${callable?.signature()
            .orEmpty()}/$target/${extra.orEmpty()}"

    public companion object {
        public val topLevel: DRI = DRI()
    }
}

public object EnumEntryDRIExtra: DRIExtraProperty<EnumEntryDRIExtra>()

public abstract class DRIExtraProperty<T> {
    public val key: String = this::class.qualifiedName
        ?: (this.javaClass.let { it.`package`.name + "." + it.simpleName.ifEmpty { "anonymous" } })
}


public class DRIExtraContainer(public val data: String? = null) {
    public val map: MutableMap<String, Any> = if (data != null) OBJECT_MAPPER.readValue(data) else mutableMapOf()
    public inline operator fun <reified T> get(prop: DRIExtraProperty<T>): T? =
        map[prop.key]?.let { prop as? T }

    public inline operator fun <reified T> set(prop: DRIExtraProperty<T>, value: T) {
        map[prop.key] = value as Any
    }

    public fun encode(): String = OBJECT_MAPPER.writeValueAsString(map)

    private companion object {
        private val OBJECT_MAPPER = ObjectMapper()
    }
}

public val DriOfUnit: DRI = DRI("kotlin", "Unit")
public val DriOfAny: DRI = DRI("kotlin", "Any")

public fun DRI.withClass(name: String): DRI = copy(classNames = if (classNames.isNullOrBlank()) name else "$classNames.$name")

public fun DRI.withTargetToDeclaration(): DRI = copy(target = PointingToDeclaration)

public fun DRI.withEnumEntryExtra(): DRI = copy(
    extra = DRIExtraContainer(this.extra).also { it[EnumEntryDRIExtra] = EnumEntryDRIExtra }.encode()
)

public val DRI.parent: DRI
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

public val DRI.sureClassNames: String
    get() = classNames ?: throw IllegalStateException("Malformed DRI. It requires classNames in this context.")

public data class Callable(
    val name: String,
    val receiver: TypeReference? = null,
    val params: List<TypeReference>,
    @property:ExperimentalDokkaApi
    val contextParameters: List<TypeReference> = emptyList(),
    val isProperty: Boolean = false
) {
    init {
        if (isProperty) require(
            params.isEmpty(),
            { "Callable $name is property, but has the following parameters: $params" })

    }

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        name: String,
        receiver: TypeReference? = null,
        params: List<TypeReference>
    ) : this(name, receiver, params, emptyList())

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    public fun copy(
        name: String = this.name,
        receiver: TypeReference? = this.receiver,
        params: List<TypeReference> = this.params
    ): Callable = Callable(name, receiver, params)

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        name: String,
        receiver: TypeReference? = null,
        params: List<TypeReference>,
        contextParameters: List<TypeReference>,
    ) : this(name, receiver, params, contextParameters)

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    public fun copy(
        name: String = this.name,
        receiver: TypeReference? = this.receiver,
        params: List<TypeReference> = this.params,
        contextParameters: List<TypeReference>
    ): Callable = Callable(name, receiver, params, contextParameters)

    public fun signature(): String {
        /**
         * Compatibility with [signature] without context parameters must be preserved,
         * as package-list (e.g. `DefaultExternalLocationProvider` in the base plugin) and unit tests
         * rely on `dri.toString`
         */
        val contextParameters = @OptIn(ExperimentalDokkaApi::class) contextParameters
        return if (contextParameters.isNotEmpty())
            "${contextParameters.joinToString("#")}#${receiver?.toString().orEmpty()}#${params.joinToString("#")}"
        else
            "${receiver?.toString().orEmpty()}#${params.joinToString("#")}"
    }

    public companion object
}

@JsonTypeInfo(use = CLASS)
public sealed class TypeReference {
    public companion object
}

public data class JavaClassReference(val name: String) : TypeReference() {
    override fun toString(): String = name
}

public data class TypeParam(
    val bounds: List<TypeReference>,
    val name: String,
) : TypeReference() {

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        bounds: List<TypeReference>,
    ) : this(bounds, "")


    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    public fun copy(
        bounds: List<TypeReference>
    ): TypeParam = copy(bounds = bounds, name = name)

    /**
     * Custom `toString` with no `name` parameter to keep compatibility of
     * `DRI.toString` calls used in package-lists and HTML rendering
     */
    override fun toString(): String = "TypeParam(bounds=${bounds})"
}


public data class TypeConstructor(
    val fullyQualifiedName: String,
    val params: List<TypeReference>
) : TypeReference() {
    override fun toString(): String = fullyQualifiedName +
            (if (params.isNotEmpty()) "[${params.joinToString(",")}]" else "")
}

public data class RecursiveType(val rank: Int): TypeReference() {
    override fun toString(): String = "^".repeat(rank + 1)
}

public data class Nullable(val wrapped: TypeReference) : TypeReference() {
    override fun toString(): String = "$wrapped?"
}

public data class Vararg(val elementType: TypeReference) : TypeReference() {
    override fun toString(): String = "vararg($elementType)"
}

public object StarProjection : TypeReference() {
    override fun toString(): String = "*"
}

@JsonTypeInfo(use = CLASS)
public sealed class DriTarget {
    override fun toString(): String = this.javaClass.simpleName

    public companion object
}

public data class PointingToGenericParameters(val parameterIndex: Int) : DriTarget() {
    override fun toString(): String = "PointingToGenericParameters($parameterIndex)"
}

public object PointingToDeclaration : DriTarget()

public data class PointingToCallableParameters(val parameterIndex: Int) : DriTarget() {
    override fun toString(): String = "PointingToCallableParameters($parameterIndex)"
}

@ExperimentalDokkaApi
public data class PointingToContextParameters(val parameterIndex: Int) : DriTarget() {
    override fun toString(): String = "PointingToContextParameters($parameterIndex)"
}

public fun DriTarget.nextTarget(): DriTarget = @OptIn(ExperimentalDokkaApi::class) when (this) {
    is PointingToGenericParameters -> PointingToGenericParameters(this.parameterIndex + 1)
    is PointingToCallableParameters -> PointingToCallableParameters(this.parameterIndex + 1)
    is PointingToContextParameters -> PointingToCallableParameters(this.parameterIndex + 1)
    is PointingToDeclaration -> this
}