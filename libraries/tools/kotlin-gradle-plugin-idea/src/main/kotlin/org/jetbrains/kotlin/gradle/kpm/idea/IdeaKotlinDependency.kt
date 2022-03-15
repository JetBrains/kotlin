/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.KotlinExternalModelContainer
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinDependency.Companion.CLASSPATH_BINARY_TYPE
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinDependency.Companion.DOCUMENTATION_BINARY_TYPE
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinDependency.Companion.SOURCES_BINARY_TYPE
import java.io.File
import java.io.Serializable

sealed interface IdeaKotlinDependency : Serializable {
    val coordinates: IdeaKotlinDependencyCoordinates?
    val external: KotlinExternalModelContainer

    companion object {
        const val CLASSPATH_BINARY_TYPE = "org.jetbrains.binary.type.classpath"
        const val SOURCES_BINARY_TYPE = "org.jetbrains.binary.type.sources"
        const val DOCUMENTATION_BINARY_TYPE = "org.jetbrains.binary.type.documentation"
    }
}

sealed interface IdeaKotlinDependencyCoordinates : Serializable

sealed interface IdeaKotlinSourceCoordinates : IdeaKotlinDependencyCoordinates {
    val buildId: String
    val projectPath: String
    val projectName: String
    val kotlinModuleName: String
    val kotlinModuleClassifier: String?
    val kotlinFragmentName: String
}

sealed interface IdeaKotlinSourceDependency : IdeaKotlinDependency {
    override val coordinates: IdeaKotlinSourceCoordinates
}

sealed interface IdeaKotlinBinaryCoordinates : IdeaKotlinDependencyCoordinates {
    val group: String
    val module: String
    val version: String
    val kotlinModuleName: String?
    val kotlinFragmentName: String?
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
data class IdeaKotlinSourceDependencyImpl(
    override val coordinates: IdeaKotlinSourceCoordinates,
    override val external: KotlinExternalModelContainer = KotlinExternalModelContainer.Empty
) : IdeaKotlinSourceDependency {

    override fun toString(): String {
        return "source: $coordinates"
    }

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinBinaryCoordinatesImpl(
    override val group: String,
    override val module: String,
    override val version: String,
    override val kotlinModuleName: String? = null,
    override val kotlinFragmentName: String? = null
) : IdeaKotlinBinaryCoordinates {

    override fun toString(): String {
        return "$group:$module:$version" +
                (if (kotlinModuleName != null) ":$kotlinModuleName" else "") +
                (if (kotlinFragmentName != null) ":$kotlinFragmentName" else "")
    }

    companion object {
        private const val serialVersionUID = 0L
    }
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinSourceCoordinatesImpl(
    override val buildId: String,
    override val projectPath: String,
    override val projectName: String,
    override val kotlinModuleName: String,
    override val kotlinModuleClassifier: String?,
    override val kotlinFragmentName: String
) : IdeaKotlinSourceCoordinates {

    override fun toString(): String = path

    companion object {
        private const val serialVersionUID = 0L
    }
}

val IdeaKotlinSourceCoordinates.path: String
    get() = "${buildId.takeIf { it != ":" }.orEmpty()}$projectPath/$kotlinModuleName/$kotlinFragmentName"

@InternalKotlinGradlePluginApi
data class IdeaKotlinResolvedBinaryDependencyImpl(
    override val coordinates: IdeaKotlinBinaryCoordinates?,
    override val binaryType: String,
    override val binaryFile: File,
    override val external: KotlinExternalModelContainer = KotlinExternalModelContainer.Empty
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
    override val external: KotlinExternalModelContainer = KotlinExternalModelContainer.Empty
) : IdeaKotlinUnresolvedBinaryDependency {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
