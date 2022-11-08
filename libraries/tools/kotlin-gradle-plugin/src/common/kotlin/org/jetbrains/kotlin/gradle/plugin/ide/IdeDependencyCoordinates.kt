/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

sealed class IdeDependencyCoordinates {
    /* Reflection helpers */
    val isBinaryCoordinates get() = this is IdeBinaryCoordinates
    val isSourceCoordinates get() = this is IdeSourceCoordinates
}

data class IdeBinaryCoordinates(
    val group: String,
    val module: String,
    val version: String?,
    val sourceSetName: String?
) : IdeDependencyCoordinates()

data class IdeSourceCoordinates(
    val buildId: String,
    val projectPath: String,
    val projectName: String,
    val sourceSetName: String
) : IdeDependencyCoordinates()