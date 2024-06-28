/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.initialization.IncludedBuild
import java.io.File
import java.io.Serializable

data class ExternalGradleDependency(
    val dependency: ResolvedDependency,
    val artifact: ResolvedArtifact
) : Serializable

data class FileCollectionExternalGradleDependency(
    val files: Collection<File>,
    val dependencyVersion: String?
) : Serializable

data class FileExternalGradleDependency(
    val dependencyName: String,
    val dependencyVersion: String,
    val file: File
) : Serializable

data class CompositeDependency(
    val dependencyName: String,
    val dependencyVersion: String,
    val includedBuildDir: File,
    @Transient
    val includedBuild: IncludedBuild?
) : Serializable

data class InternalDependency(
    val projectPath: String,
    val compilationName: String,
    val projectName: String
) : Serializable