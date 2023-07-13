/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("ConstPropertyName", "DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.gradle.idea.tcs

import java.io.Serializable

@IdeaKotlinModel
sealed interface IdeaKotlinDependencyCoordinates : Serializable

class IdeaKotlinBinaryCoordinates(
    val group: String,
    val module: String,
    val version: String?,
    val sourceSetName: String? = null,

    /**
     * @see IdeaKotlinBinaryCapability
     * @since 1.9.20
     */
    val capabilities: Set<IdeaKotlinBinaryCapability> = emptySet(),

    /**
     * @see IdeaKotlinBinaryAttributes
     * @since 1.9.20
     */
    val attributes: IdeaKotlinBinaryAttributes = IdeaKotlinBinaryAttributes(),
) : IdeaKotlinDependencyCoordinates {

    constructor(
        group: String,
        module: String,
        version: String?,
        sourceSetName: String? = null,
    ) : this(
        group = group,
        module = module,
        version = version,
        sourceSetName = sourceSetName,
        capabilities = emptySet()
    )


    /**
     * String that can be used to identify the coordinates with the following contract:
     * All coordinates that are equal will have an equal identityString
     * All coordinates that are not equal will have a non equal identityString
     *
     * This, however, shall not be shown to users, as the String is hard to read
     * The identityString is not stable across Kotlin versions, hence not intended to be parsed.
     */
    val identityString: String
        get() = buildString {
            append("$group:$module")
            if (sourceSetName != null) append(":$sourceSetName")
            if (version != null) append(":$version")
            if (capabilities.isNotEmpty()) {
                append(capabilities.joinToString(", ", "(", ")"))
            }
            if (attributes.isNotEmpty()) {
                append("+attributes(${attributes.hashCode()})")
            }
        }

    /**
     * String intended to be shown to users. E.g. a library within the IDE can use
     * this String to show the coordinates to a given user.
     *
     * This will try to be as useful as possible, implementing a notion of 'classifying capabilities'
     *
     * Example, testFixtures from Gradle:
     * Such testFixtures will be resolved with coordinates like
     * ```kotlin
     * IdeaKotlinBinaryCoordinates(
     *     group = "org.jetbrains",
     *     module = "sample",
     *     version = "1.0.0,
     *     capabilities = setOf(
     *         IdeaKotlinBinaryCapability(
     *             group = "org.jetbrains",
     *             name = "sample-test-fixtures,  /* <--- See the classifying 'test-fixtures' suffix */
     *             version = "1.0.0"
     *         )
     *     )
     * )
     * ```
     *
     * Such coordinates will be displayed using their classifiers like
     * "org.jetbrains:sample-test-fixtures:1.0.0"
     *
     */
    val displayString: String
        get() {
            val classifyingCapabilities =
                capabilities.filter { capability -> capability.group == group && capability.name.startsWith(module) }
            return when {
                classifyingCapabilities.size == 1 -> buildString {
                    append(classifyingCapabilities.single())
                    if (sourceSetName != null) append(":$sourceSetName")
                }
                classifyingCapabilities.size > 1 -> buildString {
                    append("$group:$module-")
                    append(classifyingCapabilities.joinToString(prefix = "(", postfix = ")", separator = ", ") { capability ->
                        capability.name.removePrefix(module).removePrefix("-")
                    })
                    if (sourceSetName != null) append(":$sourceSetName")
                    if (version != null) append(":$version")
                }
                else -> buildString {
                    append("$group:$module")
                    if (sourceSetName != null) append(":$sourceSetName")
                    if (version != null) append(":$version")
                }
            }
        }

    override fun toString(): String = displayString

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IdeaKotlinBinaryCoordinates) return false
        if (group != other.group) return false
        if (module != other.module) return false
        if (version != other.version) return false
        if (sourceSetName != other.sourceSetName) return false
        if (capabilities != other.capabilities) return false
        if (attributes != other.attributes) return false
        return true
    }

    override fun hashCode(): Int {
        var result = group.hashCode()
        result = 31 * result + module.hashCode()
        result = 31 * result + (version?.hashCode() ?: 0)
        result = 31 * result + (sourceSetName?.hashCode() ?: 0)
        result = 31 * result + capabilities.hashCode()
        result = 31 * result + attributes.hashCode()
        return result
    }

    /*
    Keep compatibility with 1.9.0 and lower
     */
    fun copy(
        group: String = this.group,
        module: String = this.module,
        version: String? = this.version,
        sourceSetName: String? = this.sourceSetName,
    ): IdeaKotlinBinaryCoordinates {
        return IdeaKotlinBinaryCoordinates(
            group = group,
            module = module,
            version = version,
            sourceSetName = sourceSetName,
            capabilities = capabilities,
            attributes = attributes,
        )
    }

    fun copy(
        group: String = this.group,
        module: String = this.module,
        version: String? = this.version,
        sourceSetName: String? = this.sourceSetName,
        capabilities: Set<IdeaKotlinBinaryCapability> = this.capabilities,
        attributes: IdeaKotlinBinaryAttributes = this.attributes,
    ): IdeaKotlinBinaryCoordinates {
        return IdeaKotlinBinaryCoordinates(
            group = group,
            module = module,
            version = version,
            sourceSetName = sourceSetName,
            capabilities = capabilities,
            attributes = attributes
        )
    }

    @Deprecated("Since 1.9.20")
    operator fun component1(): String = this.group

    @Deprecated("Since 1.9.20")
    operator fun component2(): String = this.module

    @Deprecated("Since 1.9.20")
    operator fun component3(): String? = this.version

    @Deprecated("Since 1.9.20")
    operator fun component4(): String? = this.sourceSetName

    /**
     * In order to keep java.io.Serializable implementation backwards compatible:
     * 'capabilities' was added in 1.9.20. If a binary produced before 1.9.20 gets deserialized, then 'capabilities'
     * will be 'null'. In this case we use the 'copy' function to provide an instance that will have an emptySet instead.
     */
    private fun readResolve(): Any {
        @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
        if (capabilities == null || attributes == null) {
            return copy(
                capabilities = capabilities ?: emptySet(),
                attributes = attributes ?: IdeaKotlinBinaryAttributes()
            )
        }

        return this
    }

    internal companion object {
        const val serialVersionUID = 0L
    }
}

data class IdeaKotlinProjectCoordinates(
    val buildId: String,
    val projectPath: String,
    val projectName: String,
) : Serializable, IdeaKotlinDependencyCoordinates {
    override fun toString(): String {
        return "${buildId.takeIf { it != ":" }?.plus(":").orEmpty()}$projectPath"
    }

    internal companion object {
        const val serialVersionUID = 0L
    }
}

data class IdeaKotlinSourceCoordinates(
    val project: IdeaKotlinProjectCoordinates,
    val sourceSetName: String,
) : IdeaKotlinDependencyCoordinates {

    val buildId: String get() = project.buildId
    val projectPath: String get() = project.projectPath
    val projectName: String get() = project.projectName

    override fun toString(): String {
        return "$project/$sourceSetName"
    }

    internal companion object {
        const val serialVersionUID = 0L
    }
}
