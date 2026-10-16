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
internal data class DRI(
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
        val topLevel: DRI = DRI()
    }
}

internal object EnumEntryDRIExtra: DRIExtraProperty<EnumEntryDRIExtra>()

internal abstract class DRIExtraProperty<T> {
    val key: String = this::class.qualifiedName
        ?: (this.javaClass.let { it.`package`.name + "." + it.simpleName.ifEmpty { "anonymous" } })
}


internal class DRIExtraContainer(val data: String? = null) {
    val map: MutableMap<String, Any> = if (data != null) OBJECT_MAPPER.readValue(data) else mutableMapOf()
    inline operator fun <reified T> get(prop: DRIExtraProperty<T>): T? =
        map[prop.key]?.let { prop as? T }

    inline operator fun <reified T> set(prop: DRIExtraProperty<T>, value: T) {
        map[prop.key] = value as Any
    }

    fun encode(): String = OBJECT_MAPPER.writeValueAsString(map)

    private companion object {
        private val OBJECT_MAPPER = ObjectMapper()
    }
}

internal val DriOfUnit: DRI = DRI("kotlin", "Unit")
internal val DriOfAny: DRI = DRI("kotlin", "Any")

internal fun DRI.withClass(name: String): DRI = copy(classNames = if (classNames.isNullOrBlank()) name else "$classNames.$name")

@Suppress("unused")
internal fun DRI.withTargetToDeclaration(): DRI = copy(target = PointingToDeclaration)

internal fun DRI.withEnumEntryExtra(): DRI = copy(
    extra = DRIExtraContainer(this.extra).also { it[EnumEntryDRIExtra] = EnumEntryDRIExtra }.encode()
)

internal val DRI.parent: DRI
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

internal val DRI.sureClassNames: String
    get() = classNames ?: throw IllegalStateException("Malformed DRI. It requires classNames in this context.")

internal data class Callable(
    val name: String,
    val receiver: TypeReference? = null,
    val params: List<TypeReference>,
    @property:ExperimentalDokkaApi
    val contextParameters: List<TypeReference> = emptyList(),
    val isProperty: Boolean = false,
    /**
     * Distinguishes callables that do not have an instance receiver from instance ones.
     *
     * `true` for:
     * - Java static methods/fields
     * - Enum synthetic declarations (`values`, `valueOf`, `entries`)
     * - Kotlin companion-block members (see [KEEP-0449](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0449-companions-block-extension.md))
     * - Kotlin companion extensions (see [KEEP-0449](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0449-companions-block-extension.md))
     *
     * @see org.jetbrains.dokka.model.IsCompanion
     */
    @property:ExperimentalDokkaApi
    val isCompanion: Boolean = false,
) {
    init {
        if (isProperty) require(
            params.isEmpty(),
            { "Callable $name is property, but has the following parameters: $params" })

    }

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(
        name: String,
        receiver: TypeReference? = null,
        params: List<TypeReference>
    ) : this(name, receiver, params, emptyList())

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    fun copy(
        name: String = this.name,
        receiver: TypeReference? = this.receiver,
        params: List<TypeReference> = this.params
    ): Callable = Callable(name, receiver, params)

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(
        name: String,
        receiver: TypeReference? = null,
        params: List<TypeReference>,
        contextParameters: List<TypeReference>,
    ) : this(name, receiver, params, contextParameters)

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    fun copy(
        name: String = this.name,
        receiver: TypeReference? = this.receiver,
        params: List<TypeReference> = this.params,
        contextParameters: List<TypeReference>
    ): Callable = Callable(name, receiver, params, contextParameters)

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(
        name: String,
        receiver: TypeReference? = null,
        params: List<TypeReference>,
        contextParameters: List<TypeReference>,
        isProperty: Boolean,
    ) : this(name, receiver, params, contextParameters, isProperty, isCompanion = false)

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    fun copy(
        name: String = this.name,
        receiver: TypeReference? = this.receiver,
        params: List<TypeReference> = this.params,
        contextParameters: List<TypeReference>,
        isProperty: Boolean,
    ): Callable = Callable(name, receiver, params, contextParameters, isProperty, isCompanion = false)

    fun signature(): String {
        /**
         * Compatibility with [signature] without context parameters must be preserved,
         * as package-list (e.g. `DefaultExternalLocationProvider` in the base plugin) and unit tests
         * rely on `dri.toString`.
         *
         * The `static` marker is appended only when [isCompanion] is `true` to preserve the existing
         * signature of regular instance callables.
         */
        val contextParameters = @OptIn(ExperimentalDokkaApi::class) contextParameters
        val base = if (contextParameters.isNotEmpty())
            "${contextParameters.joinToString("#")}#${receiver?.toString().orEmpty()}#${params.joinToString("#")}"
        else
            "${receiver?.toString().orEmpty()}#${params.joinToString("#")}"
        return if (@OptIn(ExperimentalDokkaApi::class) isCompanion) "$base/companion" else base
    }

    companion object
}

@JsonTypeInfo(use = CLASS)
internal sealed class TypeReference {
    companion object
}

internal data class JavaClassReference(val name: String) : TypeReference() {
    override fun toString(): String = name
}

internal data class TypeParam(
    val bounds: List<TypeReference>,
    val name: String,
) : TypeReference() {

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(
        bounds: List<TypeReference>,
    ) : this(bounds, "")


    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    fun copy(
        bounds: List<TypeReference>
    ): TypeParam = copy(bounds = bounds, name = name)

    /**
     * Custom `toString` with no `name` parameter to keep compatibility of
     * `DRI.toString` calls used in package-lists and HTML rendering
     */
    override fun toString(): String = "TypeParam(bounds=${bounds})"
}


internal data class TypeConstructor(
    val fullyQualifiedName: String,
    val params: List<TypeReference>
) : TypeReference() {
    override fun toString(): String = fullyQualifiedName +
            (if (params.isNotEmpty()) "[${params.joinToString(",")}]" else "")
}

internal data class RecursiveType(val rank: Int): TypeReference() {
    override fun toString(): String = "^".repeat(rank + 1)
}

internal data class Nullable(val wrapped: TypeReference) : TypeReference() {
    override fun toString(): String = "$wrapped?"
}

internal data class Vararg(val elementType: TypeReference) : TypeReference() {
    override fun toString(): String = "vararg($elementType)"
}

internal object StarProjection : TypeReference() {
    override fun toString(): String = "*"
}

@JsonTypeInfo(use = CLASS)
internal sealed class DriTarget {
    override fun toString(): String = this.javaClass.simpleName

    companion object
}

internal data class PointingToGenericParameters(val parameterIndex: Int) : DriTarget() {
    override fun toString(): String = "PointingToGenericParameters($parameterIndex)"
}

internal object PointingToDeclaration : DriTarget()

internal data class PointingToCallableParameters(val parameterIndex: Int) : DriTarget() {
    override fun toString(): String = "PointingToCallableParameters($parameterIndex)"
}

@ExperimentalDokkaApi
internal data class PointingToContextParameters(val parameterIndex: Int) : DriTarget() {
    override fun toString(): String = "PointingToContextParameters($parameterIndex)"
}

internal fun DriTarget.nextTarget(): DriTarget = @OptIn(ExperimentalDokkaApi::class) when (this) {
    is PointingToGenericParameters -> PointingToGenericParameters(this.parameterIndex + 1)
    is PointingToCallableParameters -> PointingToCallableParameters(this.parameterIndex + 1)
    is PointingToContextParameters -> PointingToCallableParameters(this.parameterIndex + 1)
    is PointingToDeclaration -> this
}
