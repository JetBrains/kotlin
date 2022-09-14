/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmDependency.Companion.CLASSPATH_BINARY_TYPE
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmDependency.Companion.DOCUMENTATION_BINARY_TYPE
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmDependency.Companion.SOURCES_BINARY_TYPE
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.emptyExtras
import java.io.File
import java.io.Serializable
import java.util.*

sealed interface IdeaKpmDependency : Serializable {
    val coordinates: IdeaKpmDependencyCoordinates?
    val extras: Extras

    companion object {
        const val CLASSPATH_BINARY_TYPE = "org.jetbrains.binary.type.classpath"
        const val SOURCES_BINARY_TYPE = "org.jetbrains.binary.type.sources"
        const val DOCUMENTATION_BINARY_TYPE = "org.jetbrains.binary.type.documentation"
    }
}

sealed interface IdeaKpmFragmentDependency : IdeaKpmDependency {
    enum class Type : Serializable {
        Regular, Friend, Refines;

        @InternalKotlinGradlePluginApi
        companion object {
            private const val serialVersionUID = 0L
        }
    }

    val type: Type
    override val coordinates: IdeaKpmFragmentCoordinates
}

sealed interface IdeaKpmBinaryDependency : IdeaKpmDependency {
    override val coordinates: IdeaKpmBinaryCoordinates?
}

sealed interface IdeaKpmUnresolvedBinaryDependency : IdeaKpmBinaryDependency {
    val cause: String?
}

sealed interface IdeaKpmResolvedBinaryDependency : IdeaKpmBinaryDependency {
    val binaryType: String
    val binaryFile: File
}

val IdeaKpmResolvedBinaryDependency.isSourcesType get() = binaryType == SOURCES_BINARY_TYPE
val IdeaKpmResolvedBinaryDependency.isDocumentationType get() = binaryType == DOCUMENTATION_BINARY_TYPE
val IdeaKpmResolvedBinaryDependency.isClasspathType get() = binaryType == CLASSPATH_BINARY_TYPE

@InternalKotlinGradlePluginApi
data class IdeaKpmFragmentDependencyImpl(
    override val type: IdeaKpmFragmentDependency.Type,
    override val coordinates: IdeaKpmFragmentCoordinates,
    override val extras: Extras = emptyExtras()
) : IdeaKpmFragmentDependency {

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
data class IdeaKpmResolvedBinaryDependencyImpl(
    override val coordinates: IdeaKpmBinaryCoordinates?,
    override val binaryType: String,
    override val binaryFile: File,
    override val extras: Extras = emptyExtras()
) : IdeaKpmResolvedBinaryDependency {

    override fun toString(): String {
        return "${binaryType.split(".").last()}://$coordinates/${binaryFile.name}"
    }

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}

@InternalKotlinGradlePluginApi
data class IdeaKpmUnresolvedBinaryDependencyImpl(
    override val cause: String?,
    override val coordinates: IdeaKpmBinaryCoordinates?,
    override val extras: Extras = emptyExtras()
) : IdeaKpmUnresolvedBinaryDependency {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
