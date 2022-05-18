/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinDependency.Companion.CLASSPATH_BINARY_TYPE
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinDependency.Companion.DOCUMENTATION_BINARY_TYPE
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinDependency.Companion.SOURCES_BINARY_TYPE
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.emptyExtras
import java.io.File
import java.io.Serializable
import java.util.*

sealed interface IdeaKotlinDependency : Serializable {
    val coordinates: IdeaKotlinDependencyCoordinates?
    val extras: Extras

    companion object {
        const val CLASSPATH_BINARY_TYPE = "org.jetbrains.binary.type.classpath"
        const val SOURCES_BINARY_TYPE = "org.jetbrains.binary.type.sources"
        const val DOCUMENTATION_BINARY_TYPE = "org.jetbrains.binary.type.documentation"
    }
}

sealed interface IdeaKotlinFragmentDependency : IdeaKotlinDependency {
    enum class Type : Serializable {
        Regular, Friend, Refines;

        @InternalKotlinGradlePluginApi
        companion object {
            private const val serialVersionUID = 0L
        }
    }

    val type: Type
    override val coordinates: IdeaKotlinFragmentCoordinates
}

sealed interface IdeaKotlinBinaryDependency : IdeaKotlinDependency {
    override val coordinates: IdeaKotlinBinaryCoordinates?
}

sealed interface IdeaKotlinUnresolvedBinaryDependency : IdeaKotlinBinaryDependency {
    val cause: String?
}

sealed interface IdeaKotlinResolvedBinaryDependency : IdeaKotlinBinaryDependency {
    val binaryType: String
    val binaryFile: File
}

val IdeaKotlinResolvedBinaryDependency.isSourcesType get() = binaryType == SOURCES_BINARY_TYPE
val IdeaKotlinResolvedBinaryDependency.isDocumentationType get() = binaryType == DOCUMENTATION_BINARY_TYPE
val IdeaKotlinResolvedBinaryDependency.isClasspathType get() = binaryType == CLASSPATH_BINARY_TYPE

@InternalKotlinGradlePluginApi
data class IdeaKotlinFragmentDependencyImpl(
    override val type: IdeaKotlinFragmentDependency.Type,
    override val coordinates: IdeaKotlinFragmentCoordinates,
    override val extras: Extras = emptyExtras()
) : IdeaKotlinFragmentDependency {

    override fun toString(): String {
        @Suppress("DEPRECATION")
        return "${type.name.toLowerCase(Locale.ROOT)}:$coordinates"
    }

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinResolvedBinaryDependencyImpl(
    override val coordinates: IdeaKotlinBinaryCoordinates?,
    override val binaryType: String,
    override val binaryFile: File,
    override val extras: Extras = emptyExtras()
) : IdeaKotlinResolvedBinaryDependency {

    override fun toString(): String {
        return "${binaryType.split(".").last()}://$coordinates/${binaryFile.name}"
    }

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinUnresolvedBinaryDependencyImpl(
    override val cause: String?,
    override val coordinates: IdeaKotlinBinaryCoordinates?,
    override val extras: Extras = emptyExtras()
) : IdeaKotlinUnresolvedBinaryDependency {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
