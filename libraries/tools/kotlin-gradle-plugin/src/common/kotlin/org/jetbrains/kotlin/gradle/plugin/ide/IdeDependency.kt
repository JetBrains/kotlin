/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.tooling.core.Extras
import java.io.File
import java.io.Serializable

sealed class IdeDependency {
    abstract val coordinates: IdeDependencyCoordinates?
    abstract val extras: Extras

    /* Reflection helper */

    val isSourceDependency: Boolean get() = this is IdeSourceDependency
    val isBinaryDependency: Boolean get() = this is IdeBinaryDependency
    val isResolvedBinaryDependency: Boolean get() = this is IdeResolvedBinaryDependency
    val isUnresolvedBinaryDependency: Boolean get() = this is IdeUnresolvedBinaryDependency

    companion object {
        const val CLASSPATH_BINARY_TYPE = "org.jetbrains.kotlin.gradle.plugin.ide.IdeDependency.classpathBinaryType"
        const val SOURCES_BINARY_TYPE = "org.jetbrains.kotlin.gradle.plugin.ide.IdeDependency.sourcesBinaryType"
        const val DOCUMENTATION_BINARY_TYPE = "org.jetbrains.kotlin.gradle.plugin.ide.IdeDependency.documentationBinaryType"
    }
}

data class IdeSourceDependency(
    val type: Type,
    override val coordinates: IdeSourceCoordinates,
    override val extras: Extras
) : IdeDependency() {
    enum class Type : Serializable {
        Regular, Friend, DependsOn
    }
}

sealed class IdeBinaryDependency : IdeDependency() {
    abstract override val coordinates: IdeBinaryCoordinates?
}

data class IdeUnresolvedBinaryDependency(
    val cause: String?,
    override val coordinates: IdeBinaryCoordinates?,
    override val extras: Extras
) : IdeBinaryDependency()

data class IdeResolvedBinaryDependency(
    val binaryType: String,
    val binaryFile: File,
    override val extras: Extras,
    override val coordinates: IdeBinaryCoordinates?
) : IdeBinaryDependency()